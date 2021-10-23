-- Sequences
CREATE SEQUENCE sq_underliers;

-- Tables
CREATE TABLE underliers
(
   id bigint NOT NULL,
   code character varying(31) NOT NULL,
   invalid boolean NULL, -- can be set to "true" at a later stage, after comparative checks determine the data is garbage

   CONSTRAINT pk_underliers_id PRIMARY KEY(id),
   CONSTRAINT uq_underliers_code UNIQUE(code)
);

CREATE TABLE stock_prices
(
   underlier bigint NOT NULL,
   day date NOT NULL,
   open double precision NOT NULL,
   high double precision NOT NULL,
   low double precision NOT NULL,
   close double precision NOT NULL,
   adjusted double precision NOT NULL, -- close price adjusted for splits and dividends
   implied double precision NULL, -- implied from option prices, can be set at a later stage
   volatility double precision NULL, -- ATM volatility implied from option prices, can be set at a later stage
   volume bigint NOT NULL,

   CONSTRAINT fk_stock_prices_underlier FOREIGN KEY(underlier) REFERENCES underliers
);

CREATE UNIQUE INDEX ix_stock_prices_underlier_day ON stock_prices(underlier, day);

CREATE TABLE option_prices
(
   underlier bigint NOT NULL,
   code character varying(64) NOT NULL,
   day date NOT NULL,
   is_call boolean NOT NULL, -- true if CALL, false if PUT
   strike double precision NOT NULL,
   maturity date NOT NULL,
   bid double precision NULL,
   ask double precision NULL,

   CONSTRAINT fk_option_prices_underlier FOREIGN KEY(underlier) REFERENCES underliers
);

CREATE INDEX ix_option_prices_underlier_day ON option_prices(underlier, day);
CREATE INDEX ix_option_prices_underlier ON option_prices(underlier);

CREATE TABLE forward_terms
(
   underlier bigint NOT NULL,
   day date NOT NULL,
   maturity date NOT NULL,
   forward double precision NULL, -- forward price
   interest double precision NULL, -- interest rate

   CONSTRAINT fk_forward_terms_underlier FOREIGN KEY(underlier) REFERENCES underliers
);

CREATE INDEX ix_forward_terms_underlier_day ON forward_terms(underlier, day);
CREATE INDEX ix_forward_terms_underlier ON forward_terms(underlier);

-- Cleanup
--DROP TABLE option_prices;
--DROP TABLE stock_prices;
--DROP TABLE underliers;
--DROP SEQUENCE sq_underliers;
