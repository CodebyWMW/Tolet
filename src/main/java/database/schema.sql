-- USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL COLLATE NOCASE UNIQUE,
    email TEXT COLLATE NOCASE UNIQUE,
    phone TEXT UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL,
    birthdate TEXT,
    profile_image TEXT,
    verified INTEGER DEFAULT 0,
    public_id TEXT UNIQUE
);

-- HOUSES TABLE
CREATE TABLE IF NOT EXISTS houses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    location TEXT NOT NULL,
    rent REAL NOT NULL,
    owner_id INTEGER,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- BOOKINGS TABLE
CREATE TABLE IF NOT EXISTS bookings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    house_id INTEGER,
    tenant_id INTEGER,
    start_date TEXT,
    end_date TEXT,
    FOREIGN KEY (house_id) REFERENCES houses(id),
    FOREIGN KEY (tenant_id) REFERENCES users(id)
);

-- USERS AUDIT TABLE
CREATE TABLE IF NOT EXISTS users_audit (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    public_id TEXT,
    name TEXT,
    email TEXT,
    phone TEXT,
    role TEXT,
    deleted_at TEXT,
    deleted_by TEXT
);

-- APP IMAGES TABLE
CREATE TABLE IF NOT EXISTS app_images (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    image_key TEXT NOT NULL UNIQUE,
    image_data BLOB NOT NULL,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP
);
