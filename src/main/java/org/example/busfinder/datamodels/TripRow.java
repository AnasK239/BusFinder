package org.example.busfinder.datamodels;


public record TripRow(String tripId, String route,
                      String time, String status, String seats) {}
