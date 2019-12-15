# Ideas for a database 

Below is a very loose suggestion for structure of a future server database. 
We suggest that it be PostgreSQL run in an Alpine-based Docker container. For more information visit [https://hub.docker.com/_/postgres](https://hub.docker.com/_/postgres)

## SQL code template

The data could be stored in three tables: one for users, one for games, and one for gamestates. The corresponding (incomplete, for the sake of demonstration) SQL statements (postgres style) would be:

```sql
CREATE DATABASE chess;
```

```sql
CREATE TABLE Users(id serial PRIMARY KEY, name varchar(128) NOT NULL);
```

```sql
CREATE TABLE Games(id serial PRIMARY KEY, white_player integer NOT NULL, black_player integer NOT NULL, FOREIGN KEY (white_player) REFERENCES Users(id), FOREIGN KEY (black_player) REFERENCES Users(id));
```

```sql
CREATE TABLE States(id serial PRIMARY KEY, created timestamp DEFAULT CURRENT_TIMESTAMP, turn integer NOT NULL, state varchar(128) NOT NULL, game integer NOT NULL, FOREIGN KEY (game) REFERENCES Games(id));
```
## Examples of inserting data


```sql
INSERT INTO Users(name) VALUES('Bobby Fischer');
```

```sql
INSERT INTO Games(white_player, black_player) VALUES(2, 3);
```

```sql
INSERT INTO States(turn, state, game) VALUES(1, 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR', 1);
```
