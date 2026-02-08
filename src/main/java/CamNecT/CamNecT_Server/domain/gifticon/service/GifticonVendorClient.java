package CamNecT.CamNecT_Server.domain.gifticon.service;

import CamNecT.CamNecT_Server.domain.gifticon.model.GifticonProduct;

import java.util.List;

public interface GifticonVendorClient {
    List<GifticonProduct.VendorSnapshot> fetchProducts();
}