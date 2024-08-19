-- Sequences
CREATE SEQUENCE sq_underliers;
CREATE SEQUENCE sq_strategies;
CREATE SEQUENCE sq_trades;
CREATE SEQUENCE sq_mtm;

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

CREATE TABLE statistics
(
   underlier bigint NOT NULL,
   day date NOT NULL,
   spot_fwd_diff double precision NULL,
   parity_total double precision NULL,
   option_total double precision NULL,

   CONSTRAINT fk_statistics_underlier FOREIGN KEY(underlier) REFERENCES underliers
);

CREATE UNIQUE INDEX ix_statistics_underlier_day ON statistics(underlier, day);

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

------------------
CREATE TABLE stock_splits
(
    underlier bigint NOT NULL,
    date date NOT NULL,
    ratio double precision NOT NULL,

    CONSTRAINT fk_stock_splits_underlier FOREIGN KEY(underlier) REFERENCES underliers,
    CONSTRAINT uq_stock_splits UNIQUE(underlier, date)
);

CREATE INDEX ix_stock_splits_underlier ON stock_splits(underlier);

CREATE TABLE stock_dividends
(
    underlier bigint NOT NULL,
    date date NOT NULL,
    dividend double precision NOT NULL,

    CONSTRAINT fk_stock_dividends_underlier FOREIGN KEY(underlier) REFERENCES underliers,
    CONSTRAINT uq_stock_dividends UNIQUE(underlier, date)
);

CREATE INDEX ix_stock_dividends_underlier ON stock_dividends(underlier);

-- DROP TABLE strategies;
CREATE TABLE strategies
(
   id bigint NOT NULL,
   type character varying(64) NOT NULL,
   number int NOT NULL, -- used for easy identification in the application log
   multiplier double precision NULL, -- defaults to 1 if NULL
   underlier bigint NOT NULL,
   execution_day date NOT NULL,
   maturity_day date NULL,
   volatility double precision NOT NULL, -- volatility at entry
   execution_spot double precision NOT NULL, -- spot at entry
   expected_pnl_mean double precision NOT NULL, -- expected absolute PNL mean
   expected_pnl_dev double precision NOT NULL, -- expected absolute PNL dev
   realized_pnl double precision NULL, -- realized absolute PNL
   capital double precision NOT NULL, -- how much capital is allocated to the strategy (ex: $1000)
   commission double precision NULL, -- total accumulated commission from all the trades
   data character varying(1024) NULL, -- text-encoded binary data for custom use

   CONSTRAINT pk_strategies_id PRIMARY KEY(id),
   CONSTRAINT fk_strategies_underlier FOREIGN KEY(underlier) REFERENCES underliers
);

CREATE INDEX ix_strategies_underlier ON strategies(underlier);

CREATE TABLE trades
(
   id bigint NOT NULL,
   strategy bigint NOT NULL,
   execution_day date NOT NULL,
   instr_type character varying(32) NOT NULL,
   instr_code character varying(32) NOT NULL,
   instr_is_call boolean NULL,
   instr_maturity date NULL,
   instr_strike double precision NULL,
   quantity double precision NOT NULL,
   price double precision NOT NULL,
   tv double precision NOT NULL,
   commission double precision NULL,
   label character varying(32) NULL,
   is_static boolean NULL, -- static trades are not delta-hedged

   CONSTRAINT pk_trades_id PRIMARY KEY(id),
   CONSTRAINT fk_trades_strategy FOREIGN KEY(strategy) REFERENCES strategies
);

CREATE INDEX ix_trades_strategy ON trades(strategy);

CREATE TABLE mtm
(
    id bigint NOT NULL,
    strategy bigint NOT NULL,
    day date NOT NULL,
    delta_position double precision NULL,
    spot_price double precision NULL,
    volatility double precision NULL,
    market_profit double precision NULL, -- Profit if closing at market
    theoretical_profit double precision NULL, -- Profit if closing at TV

    CONSTRAINT pk_mtm_id PRIMARY KEY(id),
    CONSTRAINT fk_mtm_strategy FOREIGN KEY(strategy) REFERENCES strategies
);

CREATE INDEX ix_mtm_strategy ON mtm(strategy);

CREATE TABLE nav
(
   day date NOT NULL,
   strategy_type character varying(64) NOT NULL,
   underlier bigint NULL,
   available double precision NOT NULL,
   allocated double precision NOT NULL,

   CONSTRAINT fk_nav_underlier FOREIGN KEY(underlier) REFERENCES underliers
);

CREATE INDEX ix_nav_strategy_type_day ON nav(strategy_type, day);
CREATE UNIQUE INDEX ix_nav_strategy_type_day_underlier ON nav(strategy_type, day, underlier);

-- Data validation query
--select
--  u.code,
--	avg(s.spot_fwd_diff) as avg_spot_fwd_diff,
--	avg(least(s.parity_total, 100)) as avg_parity_total,
--	avg(least(s.option_total, 100)) as avg_option_total
--from
--	statistics s,
--	underliers u
--where
--	s.underlier = u.id
--group by
--	u.code
--order by
--	avg_option_total
--desc;
