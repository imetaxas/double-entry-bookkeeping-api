CREATE TABLE client (
   ref VARCHAR(100) NOT NULL,
   creation_date DATE NOT NULL,

   PRIMARY KEY(ref)
);

CREATE TABLE account (
  id INTEGER GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
  client_ref VARCHAR(100) NOT NULL,
  account_ref VARCHAR(20) NOT NULL,
  amount DECIMAL(20,2) NOT NULL,
  currency VARCHAR(3) NOT NULL
);

CREATE TABLE transaction_history (
  client_ref VARCHAR(100) NOT NULL,
  transaction_ref VARCHAR(20) NOT NULL,
  transaction_type VARCHAR(20) NOT NULL,
  transaction_date DATE NOT NULL,

  PRIMARY KEY(client_ref, transaction_ref)
);

CREATE TABLE transaction_leg (
	client_ref VARCHAR(100) NOT NULL,
	transaction_ref VARCHAR(20) NOT NULL,
	account_ref VARCHAR(20) NOT NULL,
	amount DECIMAL(20,2) NOT NULL,
	currency VARCHAR(3) NOT NULL
);
