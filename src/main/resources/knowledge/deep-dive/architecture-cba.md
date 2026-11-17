---
section: architecture
company: ""
role: ""
startDate: ""
endDate: ""
skills:
  - Apache Struts
  - Apache Wicket
  - Spring
  - React.js
  - Kafka
  - PostgreSQL
  - MongoDB
  - Microsoft SQL Server
  - Docker
  - Kubernetes
  - AWS
docVersion: 1
---

Core Benefits Administration (CBA) is the platform used to run employee benefits for large employers, and Shaahil worked on it at Wipro and at Alight. It is made up of 10 to 15 applications, each with a specific job and each deployed separately. The three main ones are RI, MSS and SITE, with multiple screens each.

RI is the configuration application. Every client has its own configuration there, and RI writes it to the database. MSS (Manager Self Service, used by managers) and SITE (the interface end users use to see and choose their benefits) read that configuration. For example, changing the calculation for a retirement fund for one employer in RI shows up for that employer in MSS and in SITE.

In MSS and SITE, Kafka carries messages that come from user activity. A message triggers several calculations that produce the correct result in SITE, for example when a calculation changes or when a user chooses a different plan than usual. Kafka also carries notifications about changes, and SITE offers different notification options depending on the plans a user selects.

Legacy screens were built with Apache Struts and Apache Wicket and were migrated: Struts modules to Spring, and Wicket screens to React.js. Structured, tabular data is stored in PostgreSQL, and MongoDB holds generic, flexible data, so that changes need fewer schema updates. Microsoft SQL Server is also part of the data layer. The applications run as Docker containers on Kubernetes on AWS. Releases to production use multiple colour environments with regression testing, so that what users receive is error free.
