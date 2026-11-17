---
section: decisions
company: Alight Solutions
role: IT Software Engineer
startDate: 2025-09
endDate: 2026-01
skills:
  - Apache Struts
  - Apache Wicket
  - Spring
  - React.js
docVersion: 1
---

Moving modules from Struts to Spring was the hard part of the migration, because the logic was divided across multiple applications. Shaahil read through the code to understand the codebase, then reached a conclusion about how to move each module.

He chose React for the Wicket screens, and a large number of screens, including MSS screens, were migrated. Wicket screens were slow to load, and deployments took a very long time. React moves the screen work into the browser, so screens load faster for users. The front end is separate from the Java back end and talks to it through APIs, so a screen change no longer needs a long full-application deployment, and one JavaScript and TypeScript stack serves many screens and several applications with reusable components.

Changes that spanned applications were made in RI first, then MSS, then SITE, and shipped under multiple releases. Each deployment went to a colour-coded environment, where the client, the business analysts and the testers reviewed it, and regression testing ran before the change moved to production.
