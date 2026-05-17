import { format as formatSql } from 'sql-formatter'

const SQL_FORMATTER_OPTIONS = {
  language: 'trino',
  keywordCase: 'upper',
  tabWidth: 2
}

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

export const formatSqlText = value => {
  const input = normalizeInput(value)
  if (!input) {
    return ''
  }

  try {
    return formatSql(input, SQL_FORMATTER_OPTIONS)
      .split('\n')
      .map(line => line.trimEnd())
      .join('\n')
      .trim()
  } catch {
    return input
  }
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
        ? `<span class="sql-token sql-token-keyword">${escapeHtml(word)}</span>`
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
