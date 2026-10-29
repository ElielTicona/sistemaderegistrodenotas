-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1
-- Tiempo de generación: 23-04-2026 a las 12:18:18
-- Versión del servidor: 10.4.32-MariaDB
-- Versión de PHP: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `bd_evalinte`
--
CREATE DATABASE IF NOT EXISTS `bd_evalinte` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `bd_evalinte`;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `cursos`
--

CREATE TABLE `cursos` (
  `id_curso` int(11) NOT NULL,
  `nombre_curso` varchar(100) NOT NULL,
  `codigo_curso` varchar(20) DEFAULT NULL,
  `id_docente` int(11) DEFAULT NULL,
  `id_escuela` int(11) DEFAULT NULL,
  `semestre` varchar(5) DEFAULT NULL,
  `estado` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `cursos`
--

INSERT INTO `cursos` (`id_curso`, `nombre_curso`, `codigo_curso`, `id_docente`, `id_escuela`, `semestre`, `estado`) VALUES
(4, 'MATEMATICA', '', 2, 1, 'II', 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `detalle_respuestas_examen`
--

CREATE TABLE `detalle_respuestas_examen` (
  `id_detalle` int(11) NOT NULL,
  `dni_estudiante` char(20) DEFAULT NULL,
  `id_pregunta` int(11) DEFAULT NULL,
  `id_opcion_elegida` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `detalle_respuestas_examen`
--

INSERT INTO `detalle_respuestas_examen` (`id_detalle`, `dni_estudiante`, `id_pregunta`, `id_opcion_elegida`) VALUES
(1, '2021207001', 1, 3),
(2, '2021207001', 2, 7),
(3, '2021207001', 1, 4),
(4, '2021207001', 2, 8);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `escuelas`
--

CREATE TABLE `escuelas` (
  `id_escuela` int(11) NOT NULL,
  `nombre_escuela` varchar(100) NOT NULL,
  `estado` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `escuelas`
--

INSERT INTO `escuelas` (`id_escuela`, `nombre_escuela`, `estado`) VALUES
(1, 'INGENIERÍA DE SISTEMAS', 1),
(2, 'INGENIERÍA DE SOFTWARE', 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `estudiantes`
--

CREATE TABLE `estudiantes` (
  `dni` varchar(20) NOT NULL,
  `nombre_completo` varchar(150) NOT NULL,
  `codigo_univ` varchar(20) DEFAULT NULL,
  `id_escuela` int(11) DEFAULT NULL,
  `semestre` varchar(2) DEFAULT NULL,
  `id_usuario` int(11) DEFAULT NULL,
  `estado` tinyint(4) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `estudiantes`
--

INSERT INTO `estudiantes` (`dni`, `nombre_completo`, `codigo_univ`, `id_escuela`, `semestre`, `id_usuario`, `estado`) VALUES
('2021207001', 'PRUEBA', '2021207001', 1, 'II', 4, 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `evaluaciones`
--

CREATE TABLE `evaluaciones` (
  `id_evaluacion` int(11) NOT NULL,
  `id_curso` int(11) DEFAULT NULL,
  `titulo` varchar(100) NOT NULL,
  `tipo` enum('EXAMEN','RUBRICA') NOT NULL,
  `fecha_creacion` timestamp NOT NULL DEFAULT current_timestamp(),
  `estado` tinyint(4) DEFAULT 1,
  `dificultad` enum('BÁSICO','INTERMEDIO','AVANZADO') DEFAULT 'BÁSICO',
  `fecha_examen` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `evaluaciones`
--

INSERT INTO `evaluaciones` (`id_evaluacion`, `id_curso`, `titulo`, `tipo`, `fecha_creacion`, `estado`, `dificultad`, `fecha_examen`) VALUES
(1, 4, 'Examen Prueba', 'EXAMEN', '2026-04-23 09:41:18', 1, 'BÁSICO', '2026-04-24'),
(2, 4, 'Rubrica Prueba', 'RUBRICA', '2026-04-23 09:41:29', 1, 'BÁSICO', '2026-04-24');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `matricula`
--

CREATE TABLE `matricula` (
  `id_matricula` int(11) NOT NULL,
  `id_curso` int(11) DEFAULT NULL,
  `dni_estudiante` varchar(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `matricula`
--

INSERT INTO `matricula` (`id_matricula`, `id_curso`, `dni_estudiante`) VALUES
(1, 4, '2021207001');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `notas_finales`
--

CREATE TABLE `notas_finales` (
  `id_nota` int(11) NOT NULL,
  `dni_estudiante` varchar(20) DEFAULT NULL,
  `id_evaluacion` int(11) DEFAULT NULL,
  `puntaje_obtenido` decimal(5,2) DEFAULT NULL,
  `fecha_registro` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `notas_finales`
--

INSERT INTO `notas_finales` (`id_nota`, `dni_estudiante`, `id_evaluacion`, `puntaje_obtenido`, `fecha_registro`) VALUES
(1, '2021207001', 2, 10.00, '2026-04-23 09:48:00'),
(2, '2021207001', 1, 20.00, '2026-04-23 09:58:20');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `opciones`
--

CREATE TABLE `opciones` (
  `id_opcion` int(11) NOT NULL,
  `id_pregunta` int(11) DEFAULT NULL,
  `texto_opcion` text DEFAULT NULL,
  `es_correcta` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `opciones`
--

INSERT INTO `opciones` (`id_opcion`, `id_pregunta`, `texto_opcion`, `es_correcta`) VALUES
(1, 1, 'h5y', 0),
(2, 1, 'h6o', 0),
(3, 1, 'h2o', 1),
(4, 1, 'h2j', 0),
(5, 2, 'cafe', 0),
(6, 2, 'celeste', 0),
(7, 2, 'azul', 1),
(8, 2, 'rojo', 0);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `preguntas`
--

CREATE TABLE `preguntas` (
  `id_pregunta` int(11) NOT NULL,
  `id_evaluacion` int(11) DEFAULT NULL,
  `texto_pregunta` text DEFAULT NULL,
  `puntaje_max` decimal(5,2) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `preguntas`
--

INSERT INTO `preguntas` (`id_pregunta`, `id_evaluacion`, `texto_pregunta`, `puntaje_max`) VALUES
(1, 1, 'simbolo del agua', 10.00),
(2, 1, 'Color del Cielo', 10.00);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `rubricas_criterios`
--

CREATE TABLE `rubricas_criterios` (
  `id_criterio` int(11) NOT NULL,
  `id_evaluacion` int(11) DEFAULT NULL,
  `nombre_criterio` varchar(100) DEFAULT NULL,
  `descripcion` text DEFAULT NULL,
  `puntaje_max` decimal(5,2) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `rubricas_criterios`
--

INSERT INTO `rubricas_criterios` (`id_criterio`, `id_evaluacion`, `nombre_criterio`, `descripcion`, `puntaje_max`) VALUES
(1, 2, 'Criterio 1', 'Criterio 1', 3.00),
(2, 2, 'Criterio 2', 'Criterio 2', 4.00),
(3, 2, 'Criterio 3', 'Criterio 3', 4.00),
(4, 2, 'Criterio 4', 'Criterio 4', 4.00);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `usuarios`
--

CREATE TABLE `usuarios` (
  `id_usuario` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(50) NOT NULL,
  `rol` enum('ADMIN','DOCENTE','ESTUDIANTE') NOT NULL,
  `nombre_completo` varchar(150) DEFAULT NULL,
  `estado` tinyint(4) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `usuarios`
--

INSERT INTO `usuarios` (`id_usuario`, `username`, `password`, `rol`, `nombre_completo`, `estado`) VALUES
(1, 'admin', 'admin', 'ADMIN', 'Administrador', 1),
(2, 'docente', 'docente', 'DOCENTE', 'Ing. Juan Pérez', 1),
(4, '2021207001', '2021207001', 'ESTUDIANTE', 'PRUEBA', 1);

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `cursos`
--
ALTER TABLE `cursos`
  ADD PRIMARY KEY (`id_curso`),
  ADD KEY `id_docente` (`id_docente`),
  ADD KEY `id_escuela` (`id_escuela`);

--
-- Indices de la tabla `detalle_respuestas_examen`
--
ALTER TABLE `detalle_respuestas_examen`
  ADD PRIMARY KEY (`id_detalle`),
  ADD KEY `id_pregunta` (`id_pregunta`),
  ADD KEY `id_opcion_elegida` (`id_opcion_elegida`);

--
-- Indices de la tabla `escuelas`
--
ALTER TABLE `escuelas`
  ADD PRIMARY KEY (`id_escuela`);

--
-- Indices de la tabla `estudiantes`
--
ALTER TABLE `estudiantes`
  ADD PRIMARY KEY (`dni`),
  ADD UNIQUE KEY `codigo_univ` (`codigo_univ`),
  ADD KEY `id_escuela` (`id_escuela`),
  ADD KEY `id_usuario` (`id_usuario`);

--
-- Indices de la tabla `evaluaciones`
--
ALTER TABLE `evaluaciones`
  ADD PRIMARY KEY (`id_evaluacion`),
  ADD KEY `id_curso` (`id_curso`);

--
-- Indices de la tabla `matricula`
--
ALTER TABLE `matricula`
  ADD PRIMARY KEY (`id_matricula`),
  ADD KEY `id_curso` (`id_curso`),
  ADD KEY `matricula_ibfk_2` (`dni_estudiante`);

--
-- Indices de la tabla `notas_finales`
--
ALTER TABLE `notas_finales`
  ADD PRIMARY KEY (`id_nota`),
  ADD KEY `id_evaluacion` (`id_evaluacion`),
  ADD KEY `notas_finales_ibfk_1` (`dni_estudiante`);

--
-- Indices de la tabla `opciones`
--
ALTER TABLE `opciones`
  ADD PRIMARY KEY (`id_opcion`),
  ADD KEY `id_pregunta` (`id_pregunta`);

--
-- Indices de la tabla `preguntas`
--
ALTER TABLE `preguntas`
  ADD PRIMARY KEY (`id_pregunta`),
  ADD KEY `id_evaluacion` (`id_evaluacion`);

--
-- Indices de la tabla `rubricas_criterios`
--
ALTER TABLE `rubricas_criterios`
  ADD PRIMARY KEY (`id_criterio`),
  ADD KEY `id_evaluacion` (`id_evaluacion`);

--
-- Indices de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  ADD PRIMARY KEY (`id_usuario`),
  ADD UNIQUE KEY `username` (`username`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `cursos`
--
ALTER TABLE `cursos`
  MODIFY `id_curso` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT de la tabla `detalle_respuestas_examen`
--
ALTER TABLE `detalle_respuestas_examen`
  MODIFY `id_detalle` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT de la tabla `escuelas`
--
ALTER TABLE `escuelas`
  MODIFY `id_escuela` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT de la tabla `evaluaciones`
--
ALTER TABLE `evaluaciones`
  MODIFY `id_evaluacion` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT de la tabla `matricula`
--
ALTER TABLE `matricula`
  MODIFY `id_matricula` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT de la tabla `notas_finales`
--
ALTER TABLE `notas_finales`
  MODIFY `id_nota` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT de la tabla `opciones`
--
ALTER TABLE `opciones`
  MODIFY `id_opcion` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT de la tabla `preguntas`
--
ALTER TABLE `preguntas`
  MODIFY `id_pregunta` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT de la tabla `rubricas_criterios`
--
ALTER TABLE `rubricas_criterios`
  MODIFY `id_criterio` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  MODIFY `id_usuario` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `cursos`
--
ALTER TABLE `cursos`
  ADD CONSTRAINT `cursos_ibfk_1` FOREIGN KEY (`id_docente`) REFERENCES `usuarios` (`id_usuario`) ON DELETE SET NULL,
  ADD CONSTRAINT `cursos_ibfk_2` FOREIGN KEY (`id_escuela`) REFERENCES `escuelas` (`id_escuela`) ON DELETE CASCADE;

--
-- Filtros para la tabla `detalle_respuestas_examen`
--
ALTER TABLE `detalle_respuestas_examen`
  ADD CONSTRAINT `detalle_respuestas_examen_ibfk_1` FOREIGN KEY (`id_pregunta`) REFERENCES `preguntas` (`id_pregunta`) ON DELETE CASCADE,
  ADD CONSTRAINT `detalle_respuestas_examen_ibfk_2` FOREIGN KEY (`id_opcion_elegida`) REFERENCES `opciones` (`id_opcion`) ON DELETE CASCADE;

--
-- Filtros para la tabla `estudiantes`
--
ALTER TABLE `estudiantes`
  ADD CONSTRAINT `estudiantes_ibfk_1` FOREIGN KEY (`id_escuela`) REFERENCES `escuelas` (`id_escuela`) ON DELETE CASCADE,
  ADD CONSTRAINT `estudiantes_ibfk_2` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE;

--
-- Filtros para la tabla `evaluaciones`
--
ALTER TABLE `evaluaciones`
  ADD CONSTRAINT `evaluaciones_ibfk_1` FOREIGN KEY (`id_curso`) REFERENCES `cursos` (`id_curso`) ON DELETE CASCADE;

--
-- Filtros para la tabla `matricula`
--
ALTER TABLE `matricula`
  ADD CONSTRAINT `matricula_ibfk_1` FOREIGN KEY (`id_curso`) REFERENCES `cursos` (`id_curso`) ON DELETE CASCADE,
  ADD CONSTRAINT `matricula_ibfk_2` FOREIGN KEY (`dni_estudiante`) REFERENCES `estudiantes` (`dni`);

--
-- Filtros para la tabla `notas_finales`
--
ALTER TABLE `notas_finales`
  ADD CONSTRAINT `notas_finales_ibfk_1` FOREIGN KEY (`dni_estudiante`) REFERENCES `estudiantes` (`dni`),
  ADD CONSTRAINT `notas_finales_ibfk_2` FOREIGN KEY (`id_evaluacion`) REFERENCES `evaluaciones` (`id_evaluacion`) ON DELETE CASCADE;

--
-- Filtros para la tabla `opciones`
--
ALTER TABLE `opciones`
  ADD CONSTRAINT `opciones_ibfk_1` FOREIGN KEY (`id_pregunta`) REFERENCES `preguntas` (`id_pregunta`) ON DELETE CASCADE;

--
-- Filtros para la tabla `preguntas`
--
ALTER TABLE `preguntas`
  ADD CONSTRAINT `preguntas_ibfk_1` FOREIGN KEY (`id_evaluacion`) REFERENCES `evaluaciones` (`id_evaluacion`) ON DELETE CASCADE;

--
-- Filtros para la tabla `rubricas_criterios`
--
ALTER TABLE `rubricas_criterios`
  ADD CONSTRAINT `rubricas_criterios_ibfk_1` FOREIGN KEY (`id_evaluacion`) REFERENCES `evaluaciones` (`id_evaluacion`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
