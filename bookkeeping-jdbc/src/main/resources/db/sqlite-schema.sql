CREATE TABLE IF NOT EXISTS client (
   ref TEXT NOT NULL,
   creation_date TEXT NOT NULL,

   PRIMARY KEY(ref)
);

CREATE TABLE IF NOT EXISTS account (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  client_ref TEXT NOT NULL,
  account_ref TEXT NOT NULL,
  amount TEXT NOT NULL,
  opening_amount TEXT NOT NULL,
  currency TEXT NOT NULL,
  normal_balance TEXT,
  UNIQUE (client_ref, account_ref)
);

CREATE TABLE IF NOT EXISTS transaction_history (
  client_ref TEXT NOT NULL,
  transaction_ref TEXT NOT NULL,
  transaction_type TEXT NOT NULL,
  transaction_date TEXT NOT NULL,

  PRIMARY KEY(client_ref, transaction_ref)
);

CREATE TABLE IF NOT EXISTS transaction_leg (
	client_ref TEXT NOT NULL,
	transaction_ref TEXT NOT NULL,
	account_ref TEXT NOT NULL,
	amount TEXT NOT NULL,
	currency TEXT NOT NULL,
	FOREIGN KEY (client_ref, transaction_ref)
		REFERENCES transaction_history (client_ref, transaction_ref),
	FOREIGN KEY (client_ref, account_ref)
		REFERENCES account (client_ref, account_ref)
);
