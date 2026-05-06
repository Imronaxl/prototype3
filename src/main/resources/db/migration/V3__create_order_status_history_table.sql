-- V3__create_order_status_history_table.sql
CREATE TABLE order_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

ALTER TABLE order_status_history ADD CONSTRAINT fk_history_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;
ALTER TABLE order_status_history ADD CONSTRAINT fk_history_user FOREIGN KEY (changed_by_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_history_order_id ON order_status_history(order_id);
CREATE INDEX idx_history_changed_at ON order_status_history(changed_at);
