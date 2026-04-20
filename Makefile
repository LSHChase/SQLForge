SHELL := /bin/bash

.PHONY: help build test lint coverage sonar clean

help:
	@echo "Available targets:"
	@echo "  build  - build backend and frontend modules when present"
	@echo "  test   - run repository tests when modules are present"
	@echo "  lint   - run repository lint checks"
	@echo "  coverage - generate backend coverage reports"
	@echo "  sonar  - run SonarQube scan when env vars are configured"
	@echo "  clean  - remove local build outputs"

build:
	@if [ -f pom.xml ]; then mvn -B clean package -DskipTests; else echo "skip backend build: pom.xml not found"; fi
	@if [ -f package.json ]; then npm install && npm run build; else echo "skip frontend build: package.json not found"; fi

test:
	@if [ -f pom.xml ]; then mvn -B test; else echo "skip backend tests: pom.xml not found"; fi
	@if [ -f package.json ]; then npm install && npm run test --if-present; else echo "skip frontend tests: package.json not found"; fi

lint:
	@if [ -f scripts/lint-repository-knowledge.js ]; then node scripts/lint-repository-knowledge.js; else echo "skip docs lint: script not found"; fi
	@if [ -f scripts/check-frontend-backend-separation.js ]; then node scripts/check-frontend-backend-separation.js; else echo "skip separation lint: script not found"; fi
	@if [ -f pom.xml ]; then mvn -B validate pmd:pmd checkstyle:check -DskipTests; else echo "skip backend lint: pom.xml not found"; fi
	@if [ -f package.json ]; then npm install && npm run lint --if-present; else echo "skip frontend lint: package.json not found"; fi

coverage:
	@if [ -f scripts/run-coverage.sh ]; then bash scripts/run-coverage.sh; else echo "skip coverage: script not found"; fi

sonar:
	@if [ -f scripts/run-sonar.sh ]; then bash scripts/run-sonar.sh; else echo "skip sonar: script not found"; fi

clean:
	rm -rf target dist node_modules
