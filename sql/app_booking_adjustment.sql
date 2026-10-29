-- Run only after production backup, schema/engine preflight and explicit migration approval.
-- No existing bookings, payments or refunds are rewritten by this migration.
CREATE TABLE app_booking_adjustment (
  adjustment_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  business_type CHAR(1) NOT NULL COMMENT '1 card, 3 activity',
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  activity_id BIGINT NULL,
  from_count INT NOT NULL,
  to_count INT NOT NULL,
  amount_fen BIGINT NOT NULL,
  state VARCHAR(24) NOT NULL COMMENT 'PAYMENT, REVIEW, PROCESSING, COMPLETED, REJECTED, CANCELLED',
  pay_no VARCHAR(32) NULL,
  expires_at DATETIME NULL,
  reviewer_id BIGINT NULL,
  review_note VARCHAR(500) NULL,
  reviewed_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_booking_adjustment_pay (pay_no),
  KEY ix_booking_adjustment_order (business_type, order_id, state),
  KEY ix_booking_adjustment_activity (activity_id, state),
  KEY ix_booking_adjustment_state (state, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE app_booking_adjustment_refund (
  item_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  adjustment_id BIGINT NOT NULL,
  pay_log_id BIGINT NOT NULL,
  refund_log_id BIGINT NOT NULL,
  out_trade_no VARCHAR(100) NOT NULL,
  out_refund_no VARCHAR(64) NOT NULL,
  total_fen BIGINT NOT NULL,
  refund_fen BIGINT NOT NULL,
  state VARCHAR(24) NOT NULL DEFAULT 'PREPARED',
  UNIQUE KEY uq_booking_refund_no (out_refund_no),
  UNIQUE KEY uq_booking_refund_payment (adjustment_id, pay_log_id),
  KEY ix_booking_refund_payment (pay_log_id, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
