package powerband.comandos;

import powerband.sintetizadores.Sintetizador;

public final class ComandoReconstruido implements Comando {

        private final Sintetizador destino;
        private final TipoAccion tipoAccion;
        private final double[] parametros;
        //Evitando uso fuera del paquete        
        protected ComandoReconstruido(Sintetizador destino,
                TipoAccion tipoAccion, double[] parametros) {
            this.destino = destino;
            this.tipoAccion = tipoAccion;
            this.parametros = parametros.clone();
        }

        @Override
        public void ejecutar() {
            destino.ejecutar(tipoAccion, parametros.clone());
        }

        @Override
        public Sintetizador destino() {
            return destino;
        }

        @Override
        public TipoAccion tipoAccion() {
            return tipoAccion;
        }

        @Override
        public double[] parametros() {
            return parametros.clone();
        }
    }