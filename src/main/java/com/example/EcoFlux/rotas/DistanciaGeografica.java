package com.example.EcoFlux.rotas;

public final class DistanciaGeografica {
    private DistanciaGeografica() {}

    public static double km(double latitudeA, double longitudeA, double latitudeB, double longitudeB) {
        double diferencaLatitude = Math.toRadians(latitudeB - latitudeA);
        double diferencaLongitude = Math.toRadians(longitudeB - longitudeA);
        double senoLatitude = Math.sin(diferencaLatitude / 2);
        double senoLongitude = Math.sin(diferencaLongitude / 2);
        double a = senoLatitude * senoLatitude + Math.cos(Math.toRadians(latitudeA))
                * Math.cos(Math.toRadians(latitudeB)) * senoLongitude * senoLongitude;
        return 6371.0088 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, a))));
    }
}
