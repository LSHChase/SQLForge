import { formatSqlText, highlightSql } from './sqlFormatting.mjs'

const TOKEN_MARK_CLASSES = {
  delete: 'sql-compare-token-mark--delete',
  insert: 'sql-compare-token-mark--insert'
}

const normalizeSqlText = value => String(value ?? '').replace(/\r\n?/g, '\n')

export const buildFormattedSqlDisplayText = value => {
  const raw = normalizeSqlText(value)
  const formatted = formatSqlText(raw) || raw.trim()
  return formatted
}

const splitFormattedSqlLines = value => {
  const formatted = buildFormattedSqlDisplayText(value)
  return formatted ? formatted.split('\n') : []
}

export const extractLeadingSqlComments = value => {
  const text = normalizeSqlText(value)
  const parts = []
  let cursor = 0
  let hasComment = false

  while (cursor < text.length) {
    const gapStart = cursor
    while (cursor < text.length && /[ \t\n]/.test(text[cursor])) {
      cursor += 1
    }
    const gap = text.slice(gapStart, cursor)

    if (text[cursor] === '-' && text[cursor + 1] === '-') {
      if (hasComment) {
        parts.push(gap)
      }
      const end = text.indexOf('\n', cursor)
      parts.push(text.slice(cursor, end === -1 ? text.length : end))
      cursor = end === -1 ? text.length : end
      hasComment = true
      continue
    }

    if (text[cursor] === '/' && text[cursor + 1] === '*') {
      if (hasComment) {
        parts.push(gap)
      }
      const end = text.indexOf('*/', cursor + 2)
      const commentEnd = end === -1 ? text.length : end + 2
      parts.push(text.slice(cursor, commentEnd))
      cursor = commentEnd
      hasComment = true
      continue
    }

    break
  }

  return hasComment ? parts.join('').trim() : ''
}

export const buildRecommendedSqlDisplay = (sourceSql, recommendedSql) => {
  const recommended = normalizeSqlText(recommendedSql).trim()
  if (!recommended) {
    return ''
  }

  const sourceComments = extractLeadingSqlComments(sourceSql)
  if (!sourceComments) {
    return recommended
  }

  const recommendedComments = extractLeadingSqlComments(recommended)
  if (recommendedComments === sourceComments) {
    return recommended
  }

  return `${sourceComments}\n${recommended}`
}

export const buildSqlCompareRows = (originalSql, recommendedSql) => {
  const originalLines = splitFormattedSqlLines(originalSql)
  const recommendedLines = splitFormattedSqlLines(recommendedSql)
  if (!originalLines.length && !recommendedLines.length) {
    return []
  }

  const ops = buildDiffOps(originalLines, recommendedLines)
  const rows = []
  let cursor = 0
  while (cursor < ops.length) {
    const op = ops[cursor]
    if (op.type === 'EQUAL') {
      rows.push(rowFromPair('EQUAL', op.originalLine, op.recommendedLine, op.originalIndex, op.recommendedIndex))
      cursor += 1
      continue
    }

    const group = []
    while (cursor < ops.length && ops[cursor].type !== 'EQUAL') {
      group.push(ops[cursor])
      cursor += 1
    }
    rows.push(...coalesceChangeGroup(group))
  }

  return rows.map((row, index) => ({ ...row, key: `${row.type}-${index}` }))
}

const buildDiffOps = (original, recommended) => {
  const dp = Array.from({ length: original.length + 1 }, () => Array(recommended.length + 1).fill(0))
  for (let left = original.length - 1; left >= 0; left -= 1) {
    for (let right = recommended.length - 1; right >= 0; right -= 1) {
      if (original[left] === recommended[right]) {
        dp[left][right] = dp[left + 1][right + 1] + 1
      } else {
        dp[left][right] = Math.max(dp[left + 1][right], dp[left][right + 1])
      }
    }
  }

  const ops = []
  let left = 0
  let right = 0
  while (left < original.length || right < recommended.length) {
    if (left < original.length && right < recommended.length && original[left] === recommended[right]) {
      ops.push({
        type: 'EQUAL',
        originalLine: original[left],
        recommendedLine: recommended[right],
        originalIndex: left + 1,
        recommendedIndex: right + 1
      })
      left += 1
      right += 1
    } else if (right >= recommended.length || (left < original.length && dp[left + 1][right] >= dp[left][right + 1])) {
      ops.push({
        type: 'DELETE',
        originalLine: original[left],
        originalIndex: left + 1
      })
      left += 1
    } else {
      ops.push({
        type: 'INSERT',
        recommendedLine: recommended[right],
        recommendedIndex: right + 1
      })
      right += 1
    }
  }
  return ops
}

const coalesceChangeGroup = group => {
  const deletes = group.filter(item => item.type === 'DELETE')
  const inserts = group.filter(item => item.type === 'INSERT')
  const count = Math.max(deletes.length, inserts.length)
  const rows = []
  for (let index = 0; index < count; index += 1) {
    const deleted = deletes[index]
    const inserted = inserts[index]
    if (deleted && inserted) {
      rows.push(rowFromPair('REPLACE', deleted.originalLine, inserted.recommendedLine, deleted.originalIndex, inserted.recommendedIndex))
    } else if (deleted) {
      rows.push(rowFromPair('DELETE', deleted.originalLine, '', deleted.originalIndex, null))
    } else if (inserted) {
      rows.push(rowFromPair('INSERT', '', inserted.recommendedLine, null, inserted.recommendedIndex))
    }
  }
  return rows
}

const rowFromPair = (type, originalLine, recommendedLine, originalIndex, recommendedIndex) => {
  const replaceHtml = type === 'REPLACE' ? buildReplaceHtml(originalLine, recommendedLine) : null
  return {
    type,
    originalLine: originalLine || '',
    recommendedLine: recommendedLine || '',
    originalIndex,
    recommendedIndex,
    originalHtml: replaceHtml?.originalHtml || lineHtml(originalLine),
    recommendedHtml: replaceHtml?.recommendedHtml || lineHtml(recommendedLine)
  }
}

const lineHtml = value => {
  const text = String(value ?? '')
  return text ? highlightSql(text) : '&nbsp;'
}

const buildReplaceHtml = (originalLine, recommendedLine) => {
  const originalTokens = tokenizeDiffLine(originalLine)
  const recommendedTokens = tokenizeDiffLine(recommendedLine)
  const originalMarkIndexes = new Set(originalTokens.filter(token => !token.ignorable).map(token => token.diffIndex))
  const recommendedMarkIndexes = new Set(recommendedTokens.filter(token => !token.ignorable).map(token => token.diffIndex))
  const matchedPairs = buildTokenMatches(
    originalTokens.filter(token => !token.ignorable),
    recommendedTokens.filter(token => !token.ignorable)
  )

  for (const pair of matchedPairs) {
    originalMarkIndexes.delete(pair.original.diffIndex)
    recommendedMarkIndexes.delete(pair.recommended.diffIndex)
  }

  return {
    originalHtml: renderMarkedTokens(originalTokens, originalMarkIndexes, 'delete'),
    recommendedHtml: renderMarkedTokens(recommendedTokens, recommendedMarkIndexes, 'insert')
  }
}

const buildTokenMatches = (originalTokens, recommendedTokens) => {
  const dp = Array.from({ length: originalTokens.length + 1 }, () => Array(recommendedTokens.length + 1).fill(0))
  for (let left = originalTokens.length - 1; left >= 0; left -= 1) {
    for (let right = recommendedTokens.length - 1; right >= 0; right -= 1) {
      if (originalTokens[left].key === recommendedTokens[right].key) {
        dp[left][right] = dp[left + 1][right + 1] + 1
      } else {
        dp[left][right] = Math.max(dp[left + 1][right], dp[left][right + 1])
      }
    }
  }

  const matches = []
  let left = 0
  let right = 0
  while (left < originalTokens.length && right < recommendedTokens.length) {
    if (originalTokens[left].key === recommendedTokens[right].key) {
      matches.push({ original: originalTokens[left], recommended: recommendedTokens[right] })
      left += 1
      right += 1
    } else if (dp[left + 1][right] >= dp[left][right + 1]) {
      left += 1
    } else {
      right += 1
    }
  }
  return matches
}

const tokenizeDiffLine = value => {
  const text = String(value ?? '')
  const tokens = []
  let cursor = 0
  let diffIndex = 0
  while (cursor < text.length) {
    const remaining = text.slice(cursor)
    const match =
      /^[ \t]+/.exec(remaining) ||
      /^--.*$/.exec(remaining) ||
      /^\/\*.*?\*\//.exec(remaining) ||
      /^'(?:''|\\.|[^'])*'/.exec(remaining) ||
      /^"(?:""|\\.|[^"])*"/.exec(remaining) ||
      /^`(?:``|\\.|[^`])*`/.exec(remaining) ||
      /^[A-Za-z_][A-Za-z0-9_$]*/.exec(remaining) ||
      /^\d+(?:\.\d+)?/.exec(remaining) ||
      /^(?:<=|>=|<>|!=|\|\||::)/.exec(remaining) ||
      /^[(),.;=<>*+\-/]/.exec(remaining) ||
      /^./.exec(remaining)
    const tokenText = match[0]
    const ignorable = /^[ \t]+$/.test(tokenText)
    tokens.push({
      text: tokenText,
      key: tokenText,
      ignorable,
      diffIndex: ignorable ? -1 : diffIndex
    })
    if (!ignorable) {
      diffIndex += 1
    }
    cursor += tokenText.length
  }
  return tokens
}

const renderMarkedTokens = (tokens, markIndexes, markType) =>
  tokens
    .map(token => {
      const html = highlightSql(token.text)
      if (token.ignorable || !markIndexes.has(token.diffIndex)) {
        return html
      }
      return `<span class="sql-compare-token-mark ${TOKEN_MARK_CLASSES[markType]}">${html}</span>`
    })
    .join('') || '&nbsp;'
