require("dotenv").config();
const express = require("express");
const cors = require("cors");
const mysql = require("mysql2/promise");

const app = express();
app.use(cors());
app.use(express.json());

const dbConfig = {
  host: process.env.DB_HOST,
  port: Number(process.env.DB_PORT),
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME
};

let pool;

async function initDatabase() {
  // Connect without database name first so db_mahasiswa is created if missing in Podman.
  const bootstrap = await mysql.createConnection({
    host: dbConfig.host,
    port: dbConfig.port,
    user: dbConfig.user,
    password: dbConfig.password,
  });

  await bootstrap.query(
    `CREATE DATABASE IF NOT EXISTS \`${dbConfig.database}\``
  );
  await bootstrap.end();

  pool = mysql.createPool({
    ...dbConfig,
    waitForConnections: true,
    connectionLimit: 10,
  });

  await pool.query(`
    CREATE TABLE IF NOT EXISTS mahasiswa (
      id INT PRIMARY KEY AUTO_INCREMENT,
      nim VARCHAR(20) NOT NULL UNIQUE,
      nama VARCHAR(100) NOT NULL,
      jurusan VARCHAR(50) NOT NULL,
      email VARCHAR(100) NOT NULL
    )
  `);
}

function sendError(res, statusCode, message) {
  // Android checks for the "error" key to distinguish failures from "message" successes.
  return res.status(statusCode).json({ error: message });
}

app.get("/mahasiswa", async (req, res) => {
  try {
    const [rows] = await pool.query(
      "SELECT id, nim, nama, jurusan, email FROM mahasiswa ORDER BY id DESC"
    );
    res.json(rows);
  } catch (err) {
    sendError(res, 500, "Gagal memuat data: " + err.message);
  }
});

app.get("/mahasiswa/:id", async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    return sendError(res, 400, "ID mahasiswa tidak valid");
  }

  try {
    const [rows] = await pool.query(
      "SELECT id, nim, nama, jurusan, email FROM mahasiswa WHERE id = ?",
      [id]
    );
    if (rows.length === 0) {
      return sendError(res, 404, "Data mahasiswa tidak ditemukan");
    }
    res.json(rows[0]);
  } catch (err) {
    sendError(res, 500, "Gagal memuat detail mahasiswa: " + err.message);
  }
});

app.post("/mahasiswa", async (req, res) => {
  const nim = String(req.body.nim || "").trim();
  const nama = String(req.body.nama || "").trim();
  const jurusan = String(req.body.jurusan || "").trim();
  const email = String(req.body.email || "").trim();

  if (!nim || !nama || !jurusan || !email) {
    return sendError(res, 400, "Semua kolom wajib diisi");
  }

  try {
    const [existing] = await pool.query(
      "SELECT id FROM mahasiswa WHERE nim = ?",
      [nim]
    );
    if (existing.length > 0) {
      return sendError(res, 409, "NIM sudah terdaftar");
    }

    const [result] = await pool.query(
      "INSERT INTO mahasiswa (nim, nama, jurusan, email) VALUES (?, ?, ?, ?)",
      [nim, nama, jurusan, email]
    );

    res.status(201).json({
      message: "Data berhasil ditambahkan",
      id: String(result.insertId),
    });
  } catch (err) {
    sendError(res, 500, "Gagal menambahkan data: " + err.message);
  }
});

app.put("/mahasiswa/:id", async (req, res) => {
  const id = Number(req.params.id);
  const nim = String(req.body.nim || "").trim();
  const nama = String(req.body.nama || "").trim();
  const jurusan = String(req.body.jurusan || "").trim();
  const email = String(req.body.email || "").trim();

  if (!Number.isInteger(id) || id <= 0) {
    return sendError(res, 400, "ID mahasiswa tidak valid");
  }
  if (!nim || !nama || !jurusan || !email) {
    return sendError(res, 400, "Semua kolom wajib diisi");
  }

  try {
    const [current] = await pool.query(
      "SELECT id FROM mahasiswa WHERE id = ?",
      [id]
    );
    if (current.length === 0) {
      return sendError(res, 404, "Data tidak ditemukan");
    }

    const [duplicate] = await pool.query(
      "SELECT id FROM mahasiswa WHERE nim = ? AND id != ?",
      [nim, id]
    );
    if (duplicate.length > 0) {
      return sendError(res, 409, "NIM sudah digunakan mahasiswa lain");
    }

    await pool.query(
      "UPDATE mahasiswa SET nim = ?, nama = ?, jurusan = ?, email = ? WHERE id = ?",
      [nim, nama, jurusan, email, id]
    );

    res.json({ message: "Data berhasil diupdate" });
  } catch (err) {
    sendError(res, 500, "Gagal mengupdate data: " + err.message);
  }
});

app.delete("/mahasiswa/:id", async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    return sendError(res, 400, "ID mahasiswa tidak valid");
  }

  try {
    const [result] = await pool.query("DELETE FROM mahasiswa WHERE id = ?", [
      id,
    ]);
    if (result.affectedRows === 0) {
      return sendError(res, 404, "Data tidak ditemukan");
    }

    res.json({ message: "Data berhasil dihapus" });
  } catch (err) {
    sendError(res, 500, "Gagal menghapus data: " + err.message);
  }
});

const port = Number(process.env.PORT || 3000);

initDatabase()
  .then(() => {
    app.listen(port, "0.0.0.0", () => {
      console.log(
        `Server CRUD Mahasiswa berjalan di http://127.0.0.1:${port} (MySQL: ${dbConfig.host}:${dbConfig.port}/${dbConfig.database})`
      );
    });
  })
  .catch((err) => {
    console.error(
      "Gagal terhubung ke MySQL Podman. Periksa file backend/.env:",
      err.message
    );
    process.exit(1);
  });

