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

export const highlightSql = value => {
  const text = String(value ?? '')
  
  if (text.length > 30000) {
    return escapeHtml(text)
  }

  // 统一的全局分词正则表达式
  const tokenRegex = /(--[^\n]*|\/\*[\s\S]*?\*\/|'(?:[^'\\]|\\.|'')*'|"(?:[^"\\]|\\.|"")*"|`(?:[^`\\]|\\.|``)*`|\d+(?:\.\d+)?|[A-Za-z_][A-Za-z0-9_$]*|[(),.;=<>*+-]+|[^\sA-Za-z0-9_$(),.;=<>*+-]+|\s+)/g

  let match
  let html = ''
  
  while ((match = tokenRegex.exec(text)) !== null) {
    const token = match[0]
    if (token.startsWith('--')) {
      html += `<span class="sql-token sql-token-comment">${escapeHtml(token)}</span>`
    } else if (token.startsWith('/*')) {
      html += `<span class="sql-token sql-token-comment">${escapeHtml(token)}</span>`
    } else if (token.startsWith("'") || token.startsWith('"') || token.startsWith('`')) {
      html += `<span class="sql-token sql-token-literal">${escapeHtml(token)}</span>`
    } else if (/^\d+(?:\.\d+)?$/.test(token)) {
      html += `<span class="sql-token sql-token-number">${escapeHtml(token)}</span>`
    } else if (/^[A-Za-z_][A-Za-z0-9_$]*$/.test(token)) {
      const upper = token.toUpperCase()
      if (KEYWORDS.has(upper)) {
        html += `<span class="sql-token sql-token-keyword">${escapeHtml(token)}</span>`
      } else {
        html += `<span class="sql-token sql-token-identifier">${escapeHtml(token)}</span>`
      }
    } else if (/^[(),.;=<>*+-]+$/.test(token)) {
      for (let i = 0; i < token.length; i++) {
        html += `<span class="sql-token sql-token-operator">${escapeHtml(token[i])}</span>`
      }
    } else {
      html += escapeHtml(token)
    }
  }
  return html
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
