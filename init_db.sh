CREATE KEYSPACE dblsha WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };
USE dblsha;
CREATE TABLE payment_requests (
  id text PRIMARY KEY,
  payment_request_hash text,
  payment_request text,
  ack_memo text,
  addr text,
  amount bigint,
);
