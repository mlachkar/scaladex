CREATE TABLE projects (
  id           CHAR(36)     NOT NULL PRIMARY KEY,
  organization VARCHAR(30)  NOT NULL,
  repository   VARCHAR(30)  NOT NULL
);
