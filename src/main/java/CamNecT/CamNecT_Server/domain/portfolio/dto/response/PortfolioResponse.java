package CamNecT.CamNecT_Server.domain.portfolio.dto.response;

public record PortfolioResponse<T>(
        boolean isMine,
        T data
) {
    public static <T> PortfolioResponse<T> of(boolean isMine, T data) {
        return new PortfolioResponse<>(isMine, data);
    }
}
