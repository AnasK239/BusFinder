package org.example.busfinder.datamodels;

public record TripRowManage(
                            String tripId,
                            String date,
                            String time,
                            String driver,
                            String bus,
                            String route,
                            String fare,
                            String status) {
}
