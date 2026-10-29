-- SISTEMA DE EVALUACION | Modulo de examenes impresos, migracion V1
-- Ejecutar despues de importar bd_evalinte.sql. Hacer backup antes.
-- No elimina tablas ni registros existentes. Compatible MariaDB 10.4+.
USE bd_evalinte;

CREATE TABLE IF NOT EXISTS lotes_examen_impreso (
 id_lote BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 id_evaluacion INT NOT NULL,
 id_docente INT NOT NULL,
 numero_generacion INT NOT NULL,
 mezclar_preguntas TINYINT(1) NOT NULL DEFAULT 1,
 mezclar_opciones TINYINT(1) NOT NULL DEFAULT 1,
 fecha_generacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 estado ENUM('PREPARANDO','LISTO','ERROR','ANULADO') NOT NULL DEFAULT 'PREPARANDO',
 ruta_pdf VARCHAR(1024) NULL,
 UNIQUE KEY uq_lote_generacion (id_evaluacion,numero_generacion),
 KEY idx_lote_docente (id_docente),
 CONSTRAINT fk_lote_eval FOREIGN KEY (id_evaluacion) REFERENCES evaluaciones(id_evaluacion) ON DELETE RESTRICT,
 CONSTRAINT fk_lote_docente FOREIGN KEY (id_docente) REFERENCES usuarios(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ejemplares_examen (
 id_ejemplar BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 id_lote BIGINT NOT NULL,
 dni_estudiante VARCHAR(20) NOT NULL,
 codigo_visible VARCHAR(32) NOT NULL,
 token_qr VARCHAR(64) NOT NULL,
 nombre_impreso VARCHAR(150) NOT NULL,
 codigo_univ_impreso VARCHAR(20),
 estado ENUM('PENDIENTE','CALIFICADO','ANULADO') NOT NULL DEFAULT 'PENDIENTE',
 UNIQUE KEY uq_ejemplar_alumno (id_lote,dni_estudiante),
 UNIQUE KEY uq_ejemplar_visible (codigo_visible),
 UNIQUE KEY uq_ejemplar_qr (token_qr),
 KEY idx_ejemplar_alumno (dni_estudiante),
 CONSTRAINT fk_ejemplar_lote FOREIGN KEY (id_lote) REFERENCES lotes_examen_impreso(id_lote) ON DELETE RESTRICT,
 CONSTRAINT fk_ejemplar_alumno FOREIGN KEY (dni_estudiante) REFERENCES estudiantes(dni) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ejemplar_preguntas (
 id_ejemplar_pregunta BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 id_ejemplar BIGINT NOT NULL,
 id_pregunta INT NULL,
 numero_impreso INT NOT NULL,
 enunciado_impreso TEXT NOT NULL,
 puntaje_impreso DECIMAL(8,2) NOT NULL,
 UNIQUE KEY uq_pregunta_posicion (id_ejemplar,numero_impreso),
 CONSTRAINT fk_ep_ejemplar FOREIGN KEY (id_ejemplar) REFERENCES ejemplares_examen(id_ejemplar) ON DELETE RESTRICT,
 CONSTRAINT fk_ep_origen FOREIGN KEY (id_pregunta) REFERENCES preguntas(id_pregunta) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ejemplar_opciones (
 id_ejemplar_opcion BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 id_ejemplar_pregunta BIGINT NOT NULL,
 id_opcion INT NULL,
 letra_impresa CHAR(1) NOT NULL,
 texto_impreso TEXT NOT NULL,
 es_correcta TINYINT(1) NOT NULL,
 UNIQUE KEY uq_opcion_letra (id_ejemplar_pregunta,letra_impresa),
 CONSTRAINT fk_eo_pregunta FOREIGN KEY (id_ejemplar_pregunta) REFERENCES ejemplar_preguntas(id_ejemplar_pregunta) ON DELETE RESTRICT,
 CONSTRAINT fk_eo_origen FOREIGN KEY (id_opcion) REFERENCES opciones(id_opcion) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS escaneos_examen (
 id_escaneo BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 id_ejemplar BIGINT NOT NULL,
 id_docente INT NOT NULL,
 ruta_imagen_original VARCHAR(1024) NOT NULL,
 fecha_escaneo TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 estado ENUM('REVISION','CONFIRMADO','RECHAZADO') NOT NULL DEFAULT 'REVISION',
 confianza_alineacion DECIMAL(5,4) NULL,
 texto_ocr VARCHAR(150) NULL,
 advertencia VARCHAR(300) NULL,
 UNIQUE KEY uq_escaneo_id_ejemplar (id_escaneo,id_ejemplar),
 KEY idx_escaneos_ejemplar (id_ejemplar),
 CONSTRAINT fk_es_ejemplar FOREIGN KEY (id_ejemplar) REFERENCES ejemplares_examen(id_ejemplar) ON DELETE RESTRICT,
 CONSTRAINT fk_es_docente FOREIGN KEY (id_docente) REFERENCES usuarios(id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS respuestas_escaneadas (
 id_respuesta_escaneada BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 id_escaneo BIGINT NOT NULL,
 id_ejemplar_pregunta BIGINT NOT NULL,
 letra_detectada CHAR(1) NULL,
 estado_lectura ENUM('VALIDA','BLANCO','MULTIPLE','AMBIGUA') NOT NULL,
 confianza DECIMAL(5,4) NOT NULL DEFAULT 0,
 letra_confirmada CHAR(1) NULL,
 corregida_manualmente TINYINT(1) NOT NULL DEFAULT 0,
 UNIQUE KEY uq_respuesta_por_escaneo (id_escaneo,id_ejemplar_pregunta),
 CONSTRAINT fk_re_escaneo FOREIGN KEY (id_escaneo) REFERENCES escaneos_examen(id_escaneo) ON DELETE RESTRICT,
 CONSTRAINT fk_re_pregunta FOREIGN KEY (id_ejemplar_pregunta) REFERENCES ejemplar_preguntas(id_ejemplar_pregunta) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Solo se ejecuta una vez en una base original. Si ya existe, no repetir esta sentencia.
ALTER TABLE notas_finales ADD COLUMN id_ejemplar BIGINT NULL;
ALTER TABLE notas_finales
 ADD UNIQUE KEY uq_notas_ejemplar (id_ejemplar),
 ADD CONSTRAINT fk_nota_ejemplar FOREIGN KEY (id_ejemplar) REFERENCES ejemplares_examen(id_ejemplar) ON DELETE RESTRICT;
