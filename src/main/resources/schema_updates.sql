-- This script updates the database schema for new features.
-- Please execute these commands on your database.

-- Step 1: Add seller_id to the items table to track who created the item.
-- This assumes you have a 'users' table with an 'id' column.
ALTER TABLE items ADD COLUMN seller_id VARCHAR(255);

-- Step 2: Create a new table to manage auto-bidding configurations.
-- This table will store the maximum bid a user is willing to place on an item.
CREATE TABLE autobids (
    id VARCHAR(255) PRIMARY KEY,
    item_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    max_amount DECIMAL(10, 2) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE (item_id, user_id) -- A user can only have one auto-bid setting per item.
);

-- End of script.
