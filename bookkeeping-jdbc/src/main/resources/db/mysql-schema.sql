CREATE TABLE IF NOT EXISTS client (
   ref VARCHAR(255) NOT NULL,
   creation_date DATETIME NOT NULL,

   PRIMARY KEY(ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS account (
  id INTEGER AUTO_INCREMENT,
  client_ref VARCHAR(255) NOT NULL,
  account_ref VARCHAR(255) NOT NULL,
  amount VARCHAR(80) NOT NULL,
  opening_amount VARCHAR(80) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  normal_balance VARCHAR(16),
  PRIMARY KEY (id),
  UNIQUE (client_ref, account_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS transaction_history (
  client_ref VARCHAR(255) NOT NULL,
  transaction_ref VARCHAR(255) NOT NULL,
  transaction_type VARCHAR(255) NOT NULL,
  transaction_date DATETIME NOT NULL,

  PRIMARY KEY(client_ref, transaction_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS transaction_leg (
	client_ref VARCHAR(255) NOT NULL,
	transaction_ref VARCHAR(255) NOT NULL,
	account_ref VARCHAR(255) NOT NULL,
	amount VARCHAR(80) NOT NULL,
	currency VARCHAR(3) NOT NULL,
	CONSTRAINT fk_leg_history FOREIGN KEY (client_ref, transaction_ref)
		REFERENCES transaction_history (client_ref, transaction_ref),
	CONSTRAINT fk_leg_account FOREIGN KEY (client_ref, account_ref)
		REFERENCES account (client_ref, account_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
