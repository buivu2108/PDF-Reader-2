package com.pdfapp.reader.point


class ServerPackage {
    var packageId: String = ""

    var price: String = ""

    var point: Int = 0

    var productionId: String = ""

    constructor(
        packageId: String = "",
        price: String = "",
        point: Int = 0,
        productId: String = ""
    ) {
        this.packageId = packageId
        this.point = point
        this.price = price
        this.productionId = productId
    }
}

