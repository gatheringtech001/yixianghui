#!/usr/bin/env python3
"""Human-reviewed travel image policy for the knowledge-base importer."""

REJECTED_ASSET_SHA256 = frozenset({
    # Yellow diamond icon variants.
    "4fb2236247859aef0a2f14cfc1102dd46bba8351ef19d6c7db52b61d8a5ec6fe",
    "ee35bb4401e6c3e7d1cf0c19406a48d7bddd9726558add728864b0ec0bcaae06",
    # Yellow location-pin icon variants.
    "1fb542a18a3cf027ef08b3ab9a66cc15ef51a09d50bb582815770e729ee66265",
    "78c676ace79d4bc9ef60806668f51ef9e01f84343797d5083fba18dd5c33c41f",
})

# Cover indices were reviewed visually. Maps, QR codes, screenshots, price
# graphics, and title cards are intentionally excluded from the first slot.
COVER_ASSET_INDEX = {
    # Kunming (17)
    "pewkt9kgx79xl8tc": 2,   # 昆明一号基地
    "aquiak2coae48qtb": 1,   # 昆明七号古滇基地
    "mzdguoxlw0qv84mo": 2,   # 昆明三号基地
    "lf7elqkx7obav66y": 2,    # 昆明九号世博基地
    "bok66ubcx62bf5vd": 3,    # 昆明二号市中心基地
    "fqfasli59nb6e0s7": 3,    # 昆明五号基地
    "qow3nk5tox0f4x0g": 3,    # 昆明八号新官渡基地
    "qtt3uco5g7royzhg": 1,    # 昆明六号温泉基地
    "nwgwhvhcfgk2gtcp": 1,    # 昆明十号基地
    "mdrcycyxm98ocqyy": 8,    # 昆明南七彩云南第一城基地
    "xeb2ipr4i7sgrgs4": 1,    # 昆明四号基地
    "hoqhgmkukoxb7kev": 1,    # 昆明安宁一号基地
    "rai5dx8c7qbx37e1": 3,    # 昆明安宁三号温泉基地
    "gdvwwh2guzzudzzc": 2,    # 昆明安宁二号温泉基地
    "ck6mzhb0ob088hrk": 2,    # 昆明寻甸旅居基地
    "pgepc3bg411flmqn": 1,    # 昆明轿子山1号基地
    "hahea8tyeagtz424": 1,    # 昆明轿子山2号基地
    # Jianshui (5)
    "hup1sfop9waagpdp": 3,
    "bicyplzadiuglw5u": 10,
    "gog3uxkl4ovgcaud": 1,
    "xlsbcg3gg7vhqbac": 1,
    "xsmmf09tufhwwckf": 1,
    # Mile (8)
    "st4kglcxn8q5ucib": 10,
    "xe1bg2opwwrgzxmb": 3,
    "iwowhw8u1078siuc": 1,
    "uum97mod9lewvde1": 2,
    "ptzlh413322upmyo": 1,
    "tsx7odoovexspgxd": 3,
    "qu6y9c0ps8z6f1vq": 12,
    "qe0abwhucil8tbw8": 9,
    # Puer (7)
    "zgo3wwq91gdw5is8": 2,
    "gh0gh93kwq9a2wve": 1,
    "sh3vq3i6zma0tv1z": 4,
    "rxut4gd125x0sdbf": 1,
    "mtg5iw95qezvyx9g": 2,
    "fo0ixlgkp5ci7r5m": 1,
    "yc0qth3g6gf8m7d8": 4,
    # Xishuangbanna (4)
    "qzmpbgulgnqvkcn9": 5,
    "rc5p8deh6t80uo7r": 10,
    "fgseitlyya1eb4wp": 5,
    "lx8hkclidsm5hvbx": 4,
    # Mangshi (4)
    "qihar3knh78vdrfi": 2,
    "qz3k6svg76h3g6s8": 1,
    "fhc5e9wqaeup05xb": 8,
    "pg6e0gg1elwew9xk": 2,
    # Tengchong (11)
    "nsl0geyypb35xhgg": 5,
    "zzlbs8ygqgtbqddw": 1,
    "pzfq6a3b5f3dn5bx": 1,
    "zes8vk1geghg38lx": 2,
    "cn0wp7y2n51lncgg": 5,
    "cqgsmhbh96uusr9l": 4,
    "khxvp6stsgdux73g": 9,
    "ldg9wbk6gghnxhwc": 1,
    "qm20rxmxwvkc80bg": 12,
    "mlwf3afitfn96y8r": 7,
    "hwil8zfmzgosy5rp": 1,
}
