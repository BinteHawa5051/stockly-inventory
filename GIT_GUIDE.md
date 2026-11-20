# Git Guide — STOCKLY

This repository was initialized with `git init -b main`. The history
below shows **real commits** created during development — verify them
yourself with the commands in each section.

## Commit history (expected)

```
* <sha>  docs: add README, test report, Javadoc and screenshots
* <sha>  test: add headless self-test and screenshot utility
* <sha>  feat: dashboard, inventory, analytics and settings UI
* <sha>  feat: product model and inventory manager with CSV persistence
* <sha>  chore: project scaffold, theme and reusable UI components
```

## Useful commands

```bash
# see the full history graph
git log --oneline --graph --all

# see what changed in the last commit
git show --stat HEAD

# working-tree status (should be clean after committing)
git status

# diff between two commits
git diff HEAD~1 HEAD

# diff of uncommitted changes
git diff
```

## Making your own commits

If you modify the project before submission:

```bash
git add -A
git commit -m "describe what changed and why"
git log --oneline   # confirm the new commit appears
```

## Pushing to GitHub

1. Create an empty repository on https://github.com/new
   (do **not** initialize it with a README — the local repo already has history).
2. From this folder:

```bash
git remote add origin https://github.com/<your-username>/<repo-name>.git
git branch -M main
git push -u origin main
```

3. Verify on GitHub that all commits and files are present.

## What is (and isn't) tracked

Tracked: all of `src/`, `README.md`, `TEST_REPORT.md`, `GIT_GUIDE.md`,
`docs/screenshots/*.png`, `docs/javadoc/**`, `sources.txt`.

Ignored (see `.gitignore`): `classes/` (build output), `lib/`,
`inventory.csv` (runtime data), IDE folders.
