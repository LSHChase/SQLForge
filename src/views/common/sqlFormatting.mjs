import { format as formatSql } from 'sql-formatter'

const SQL_FORMATTER_OPTIONS = {
  language: 'trino',
  keywordCase: 'upper',
  tabWidth: 2,
  paramTypes: { named: [':'] }
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

const escapeHtml = value => {
  const str = String(value ?? '')
  if (!/[&<>"']/.test(str)) {
    return str
  }
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

export const highlightSql = value => {
  const text = String(value ?? '')
  
  if (text.length > 250000) {
    return escapeHtml(text)
  }

  // 统一的全局分词正则表达式
  const tokenRegex = /(--[^\n]*|\/\*[\s\S]*?\*\/|'(?:[^'\\]|\\.|'')*'|"(?:[^"\\]|\\.|"")*"|`(?:[^`\\]|\\.|``)*`|\d+(?:\.\d+)?|[A-Za-z_][A-Za-z0-9_$]*|[(),.;=<>*+-]+|[^\sA-Za-z0-9_$(),.;=<>*+-]+|\s+)/g

  let match
  const htmlParts = []

  while ((match = tokenRegex.exec(text)) !== null) {
    const token = match[0]
    const char0 = token.charCodeAt(0)

    // Fast path: Whitespace tokens (no markup)
    if (char0 <= 32) {
      htmlParts.push(escapeHtml(token))
      continue
    }

    // Comment check: starting with '-' or '/'
    if (char0 === 45) { // '-'
      if (token.charCodeAt(1) === 45) { // '--'
        htmlParts.push(`<span class="sql-token sql-token-comment">${escapeHtml(token)}</span>`)
        continue
      }
    } else if (char0 === 47) { // '/'
      if (token.charCodeAt(1) === 42) { // '/*'
        htmlParts.push(`<span class="sql-token sql-token-comment">${escapeHtml(token)}</span>`)
        continue
      }
    }

    // String literals: starting with "'" or '"' or '`'
    if (char0 === 39 || char0 === 34 || char0 === 96) {
      htmlParts.push(`<span class="sql-token sql-token-literal">${escapeHtml(token)}</span>`)
      continue
    }

    // Numbers: starting with digit (0-9)
    if (char0 >= 48 && char0 <= 57) {
      htmlParts.push(`<span class="sql-token sql-token-number">${escapeHtml(token)}</span>`)
      continue
    }

    // Identifiers and Keywords: starting with letter or '_' or '$'
    if ((char0 >= 65 && char0 <= 90) || (char0 >= 97 && char0 <= 122) || char0 === 95 || char0 === 36) {
      const upper = token.toUpperCase()
      if (KEYWORDS.has(upper)) {
        htmlParts.push(`<span class="sql-token sql-token-keyword">${escapeHtml(token)}</span>`)
      } else {
        htmlParts.push(`<span class="sql-token sql-token-identifier">${escapeHtml(token)}</span>`)
      }
      continue
    }

    // Operators and Punctuation
    // regex pattern: [(),.;=<>*+-]+
    if (char0 === 40 || char0 === 41 || char0 === 44 || char0 === 46 || char0 === 59 || char0 === 61 || char0 === 60 || char0 === 62 || char0 === 42 || char0 === 43 || char0 === 45) {
      // Loop over operators and wrap each individually (to preserve original UI structure)
      for (let i = 0; i < token.length; i++) {
        htmlParts.push(`<span class="sql-token sql-token-operator">${escapeHtml(token[i])}</span>`)
      }
      continue
    }

    // Fallback
    htmlParts.push(escapeHtml(token))
  }

  return htmlParts.join('')
}

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
