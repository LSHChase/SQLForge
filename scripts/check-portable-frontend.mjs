import { spawn } from 'node:child_process'

const defaultTimeoutMs = 8000

const sleep = delayMs => new Promise(resolve => setTimeout(resolve, delayMs))

const httpGetText = async url => {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`Request failed for ${url}: ${response.status} ${response.statusText}`)
  }
  return response.text()
}

const waitForHealth = async url => {
  const startedAt = Date.now()
  while (Date.now() - startedAt < defaultTimeoutMs) {
    try {
      const body = await httpGetText(url)
      if (body.includes('"status": "UP"')) {
        return
      }
    } catch (error) {
      if (Date.now() - startedAt >= defaultTimeoutMs) {
        throw error
      }
    }
    await sleep(250)
  }
  throw new Error(`Portable frontend did not become healthy at ${url}`)
}

const main = async () => {
  const child = spawn(
    'node',
    ['dist-portable/portable-server.mjs', '--config', 'dist-portable/portable-config.json'],
    {
      stdio: ['ignore', 'pipe', 'pipe']
    }
  )

  let stderr = ''
  child.stderr.on('data', chunk => {
    stderr += chunk.toString()
  })

  try {
    await waitForHealth('http://127.0.0.1:3001/__portable/health')
    const indexBody = await httpGetText('http://127.0.0.1:3001/')
    if (!indexBody.includes('./assets/')) {
      throw new Error('Portable index.html does not use relative asset paths.')
    }
  } finally {
    child.kill('SIGINT')
    await new Promise(resolve => child.once('exit', resolve))
  }

  if (stderr.trim()) {
    console.error(stderr.trim())
  }
}

main().catch(error => {
  console.error(error.message)
  process.exit(1)
})
