# Knowledge files

Each Markdown file in this directory represents one retrievable knowledge unit, except
`quick-facts.md`, which is injected into prompts directly and must not be embedded.

Files use YAML front matter followed by a short, self-contained Markdown body. Required
front-matter fields are `section`, `company`, `role`, `startDate`, `endDate`, `skills`
(a YAML list), and `docVersion`.

Keep each body short and self-contained so it can be retrieved independently.
