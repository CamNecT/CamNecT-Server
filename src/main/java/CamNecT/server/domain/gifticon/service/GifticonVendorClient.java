package CamNecT.server.domain.gifticon.service;

import CamNecT.server.domain.gifticon.model.GifticonProduct;

import java.util.List;

public interface GifticonVendorClient {
    List<GifticonProduct.VendorSnapshot> fetchProducts();
}