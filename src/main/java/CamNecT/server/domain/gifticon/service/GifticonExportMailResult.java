package CamNecT.server.domain.gifticon.service;

public record GifticonExportMailResult(Status status, String error) {

    public enum Status { DELIVERED, NOT_SENT, UNCONFIRMED }

    public boolean successful() {
        return status == Status.DELIVERED;
    }

    public boolean requiresConfirmation() {
        return status == Status.UNCONFIRMED;
    }

    public static GifticonExportMailResult delivered() {
        return new GifticonExportMailResult(Status.DELIVERED, null);
    }

    public static GifticonExportMailResult failed(String error) {
        return new GifticonExportMailResult(Status.NOT_SENT, error);
    }

    public static GifticonExportMailResult unconfirmed(String error) {
        return new GifticonExportMailResult(Status.UNCONFIRMED, error);
    }
}
