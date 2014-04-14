CREATE KEYSPACE misito WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };
USE misito;
CREATE TABLE payment_requests (
  id text PRIMARY KEY,
  payment_request_hash text,
  payment_request text,
  ack_memo text
);
