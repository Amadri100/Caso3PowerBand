package powerband.ui;

public enum Vista {
        INICIO("Inicio", "Accesos a las áreas de trabajo.", ""),
        CONEXION("Conexión",
                "Conexión de dispositivos y sintetizadores.", "T-232"),
        DISPOSITIVOS_INSTRUMENTOS("Dispositivos e instrumentos",
                "Asignación de dispositivos a instrumentos.", "T-233"),
        SINTETIZADORES("Sintetizadores",
                "Asignación de sintetizadores a instrumentos.", "T-234"),
        CONFIGURACION("Configuración",
                "Opciones configurables de la aplicación.", "T-235"),
        GRABACION("Grabación",
                "Controles para grabar una sesión.", "T-236"),
        SESIONES("Sesiones",
                "Consulta y reproducción de sesiones.", "T-237");

        private final String etiqueta;
        private final String descripcion;
        private final String tareaResponsable;

        Vista(String etiqueta, String descripcion,
                String tareaResponsable) {
                this.etiqueta = etiqueta;
                this.descripcion = descripcion;
                this.tareaResponsable = tareaResponsable;
        }

        public String etiqueta() {
                return etiqueta;
        }

        public String descripcion() {
                return descripcion;
        }

        public String tareaResponsable() {
                return tareaResponsable;
        }
}


