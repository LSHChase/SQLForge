const KEYWORDS = new Set([
  'ADD',
  'ALTER',
  'AND',
  'AS',
  'ASC',
  'BETWEEN',
  'BY',
  'CASE',
  'CAST',
  'CREATE',
  'DELETE',
  'DESC',
  'DISTINCT',
  'DROP',
  'ELSE',
  'END',
  'EXISTS',
  'EXPLAIN',
  'FROM',
  'FULL',
  'GROUP',
  'HAVING',
  'IN',
  'INNER',
  'INSERT',
  'INTO',
  'IS',
  'JOIN',
  'LEFT',
  'LIKE',
  'LIMIT',
  'NOT',
  'NULL',
  'ON',
  'OR',
  'ORDER',
  'OUTER',
  'OVER',
  'PARTITION',
  'RIGHT',
  'SELECT',
  'SET',
  'THEN',
  'UNION',
  'UPDATE',
  'VALUES',
  'WHEN',
  'WHERE',
  'WITH'
])

const KEYWORD_PATTERN =
  /\b(select|from|where|group\s+by|order\s+by|having|limit|join|left\s+join|right\s+join|inner\s+join|full\s+join|on|and|or|with|union|case|when|then|else|end|insert\s+into|values|update|set|delete\s+from|explain)\b/gi

const CLAUSE_PATTERN =
  /\s+(FROM|WHERE|GROUP BY|ORDER BY|HAVING|LIMIT|UNION|WITH|INSERT INTO|VALUES|UPDATE|SET|DELETE FROM|EXPLAIN)\b/g

const JOIN_PATTERN = /\s+(LEFT JOIN|RIGHT JOIN|INNER JOIN|FULL JOIN|JOIN)\b/g
const BOOLEAN_PATTERN = /\s+(AND|OR)\b/g

const normalizeInput = value =>
  String(value ?? '')
    .replace(/\r\n?/g, '\n')
    .split('\n')
    .map(line => line.trimEnd())
    .join('\n')
    .trim()

const isSegmentBoundary = (text, index) =>
  text[index] === "'" ||
  text[index] === '"' ||
  text[index] === '`' ||
  (text[index] === '-' && text[index + 1] === '-') ||
  (text[index] === '/' && text[index + 1] === '*')

const readQuoted = (text, index, quote) => {
  let cursor = index + 1
  while (cursor < text.length) {
    if (text[cursor] === quote) {
      if (text[cursor + 1] === quote) {
        cursor += 2
        continue
      }
      cursor += 1
      break
    }
    if (text[cursor] === '\\') {
      cursor += 2
      continue
    }
    cursor += 1
  }
  return cursor
}

const readLineComment = (text, index) => {
  const newline = text.indexOf('\n', index)
  return newline === -1 ? text.length : newline
}

const readBlockComment = (text, index) => {
  const end = text.indexOf('*/', index + 2)
  return end === -1 ? text.length : end + 2
}

const splitSqlSegments = value => {
  const text = String(value ?? '')
  const segments = []
  let cursor = 0

  while (cursor < text.length) {
    const char = text[cursor]
    if (char === "'" || char === '"' || char === '`') {
      const end = readQuoted(text, cursor, char)
      segments.push({ type: 'literal', text: text.slice(cursor, end) })
      cursor = end
      continue
    }
    if (char === '-' && text[cursor + 1] === '-') {
      const end = readLineComment(text, cursor)
      segments.push({ type: 'comment', text: text.slice(cursor, end) })
      cursor = end
      continue
    }
    if (char === '/' && text[cursor + 1] === '*') {
      const end = readBlockComment(text, cursor)
      segments.push({ type: 'comment', text: text.slice(cursor, end) })
      cursor = end
      continue
    }

    const start = cursor
    while (cursor < text.length && !isSegmentBoundary(text, cursor)) {
      cursor += 1
    }
    segments.push({ type: 'code', text: text.slice(start, cursor) })
  }

  return segments
}

const mapSqlCode = (value, mapper) =>
  splitSqlSegments(value)
    .map(segment => (segment.type === 'code' ? mapper(segment.text) : segment.text))
    .join('')

export const formatSqlText = value => {
  const input = normalizeInput(value)
  if (!input) {
    return ''
  }

  const uppercased = mapSqlCode(input, segment =>
    segment
      .replace(/[ \t]+/g, ' ')
      .replace(/\s*,\s*/g, ', ')
      .replace(KEYWORD_PATTERN, match => match.replace(/\s+/g, ' ').toUpperCase())
  )

  const lineBroken = mapSqlCode(uppercased, segment =>
    segment
      .replace(CLAUSE_PATTERN, '\n$1')
      .replace(JOIN_PATTERN, '\n  $1')
      .replace(BOOLEAN_PATTERN, '\n    $1')
  )

  return lineBroken
    .split('\n')
    .map(line => line.trimEnd())
    .filter((line, index, lines) => line.trim() || (lines[index - 1] && lines[index - 1].trim()))
    .join('\n')
    .trim()
}

const escapeHtml = value =>
  String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')

const highlightCodeSegment = value => {
  const text = String(value ?? '')
  let cursor = 0
  let html = ''

  while (cursor < text.length) {
    const wordMatch = /^[A-Za-z_][A-Za-z0-9_$]*/.exec(text.slice(cursor))
    if (wordMatch) {
      const word = wordMatch[0]
      const upper = word.toUpperCase()
      html += KEYWORDS.has(upper)
        ? `<span class="sql-token sql-token-keyword">${escapeHtml(upper)}</span>`
        : `<span class="sql-token sql-token-identifier">${escapeHtml(word)}</span>`
      cursor += word.length
      continue
    }

    const numberMatch = /^\d+(?:\.\d+)?/.exec(text.slice(cursor))
    if (numberMatch) {
      html += `<span class="sql-token sql-token-number">${escapeHtml(numberMatch[0])}</span>`
      cursor += numberMatch[0].length
      continue
    }

    const char = text[cursor]
    if (/[(),.;=<>*+-]/.test(char)) {
      html += `<span class="sql-token sql-token-operator">${escapeHtml(char)}</span>`
    } else {
      html += escapeHtml(char)
    }
    cursor += 1
  }

  return html
}

export const highlightSql = value =>
  splitSqlSegments(value)
    .map(segment => {
      if (segment.type === 'comment') {
        return `<span class="sql-token sql-token-comment">${escapeHtml(segment.text)}</span>`
      }
      if (segment.type === 'literal') {
        return `<span class="sql-token sql-token-literal">${escapeHtml(segment.text)}</span>`
      }
      return highlightCodeSegment(segment.text)
    })
    .join('')

export const copyTextToClipboard = async value => {
  const text = String(value ?? '')
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }

  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'readonly')
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
}
