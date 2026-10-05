package com.example.data.places

data class TempleLookupPlace(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val cityState: String,
    val deity: String,
    val architecture: String,
    val nearbyFood: String,
    val defaultCategories: List<String> = listOf("cat_temples", "cat_pilgrimage")
)

object TemplePlacesCatalog {

    val FAMOUS_TEMPLES = listOf(
        TempleLookupPlace(
            name = "Brihadeeswarar Temple",
            latitude = 10.7828,
            longitude = 79.1318,
            cityState = "Thanjavur, Tamil Nadu",
            deity = "Lord Shiva (Peruvudaiyar)",
            architecture = "Chola Granite Dravidian",
            nearbyFood = "Thanjavur Ashoka Halwa, Filter Coffee"
        ),
        TempleLookupPlace(
            name = "Meenakshi Amman Temple",
            latitude = 9.9195,
            longitude = 78.1193,
            cityState = "Madurai, Tamil Nadu",
            deity = "Goddess Meenakshi & Lord Sundareswarar",
            architecture = "Nayaka Dravidian Towers",
            nearbyFood = "Madurai Jigarthanda, Bun Parotta"
        ),
        TempleLookupPlace(
            name = "Kedarnath Temple",
            latitude = 30.7352,
            longitude = 79.0669,
            cityState = "Rudraprayag, Uttarakhand",
            deity = "Lord Shiva (Kedarnath Jyotirlinga)",
            architecture = "Katyuri Himalayan Stone",
            nearbyFood = "Garhwali Mandua Roti, Chai"
        ),
        TempleLookupPlace(
            name = "Kashi Vishwanath Temple",
            latitude = 25.3109,
            longitude = 83.0107,
            cityState = "Varanasi, Uttar Pradesh",
            deity = "Lord Shiva (Vishveshwara)",
            architecture = "Nagara Spire Architecture",
            nearbyFood = "Banarasi Kachori Jalebi, Malaiyo"
        ),
        TempleLookupPlace(
            name = "Tirumala Venkateswara Temple",
            latitude = 13.6833,
            longitude = 79.3472,
            cityState = "Tirupati, Andhra Pradesh",
            deity = "Lord Venkateswara (Balaji)",
            architecture = "Dravidian Ananda Nilayam",
            nearbyFood = "Tirupati Laddu Prasadam"
        ),
        TempleLookupPlace(
            name = "Ramanathaswamy Temple",
            latitude = 9.2881,
            longitude = 79.3174,
            cityState = "Rameswaram, Tamil Nadu",
            deity = "Lord Shiva (Ramanathaswamy)",
            architecture = "Dravidian Longest Temple Corridor",
            nearbyFood = "Rameswaram Seafood & Filter Coffee"
        ),
        TempleLookupPlace(
            name = "Golden Temple (Harmandir Sahib)",
            latitude = 31.6200,
            longitude = 74.8765,
            cityState = "Amritsar, Punjab",
            deity = "Adi Granth (Holy Sikh Shrine)",
            architecture = "Sikh Gilded Marble Architecture",
            nearbyFood = "Guru ka Langar, Amritsari Kulcha"
        ),
        TempleLookupPlace(
            name = "Jagannath Temple",
            latitude = 19.8049,
            longitude = 85.8179,
            cityState = "Puri, Odisha",
            deity = "Lord Jagannath, Balabhadra, Subhadra",
            architecture = "Kalinga Architecture",
            nearbyFood = "Mahaprasad, Khaja of Puri"
        ),
        TempleLookupPlace(
            name = "Somnath Temple",
            latitude = 20.8880,
            longitude = 70.4013,
            cityState = "Veraval, Gujarat",
            deity = "Lord Shiva (First Jyotirlinga)",
            architecture = "Chalukya Nagara Architecture",
            nearbyFood = "Gujarati Thali, Kathiyawadi Khichdi"
        ),
        TempleLookupPlace(
            name = "Badrinath Temple",
            latitude = 30.7433,
            longitude = 79.4938,
            cityState = "Chamoli, Uttarakhand",
            deity = "Lord Badrinarayan (Vishnu)",
            architecture = "Traditional Himalayan Wood & Stone",
            nearbyFood = "Pahadi Dal, Fresh Apple Halwa"
        ),
        TempleLookupPlace(
            name = "Shore Temple",
            latitude = 12.6163,
            longitude = 80.1983,
            cityState = "Mahabalipuram, Tamil Nadu",
            deity = "Lord Shiva & Lord Vishnu",
            architecture = "Pallava Rock-Cut Architecture",
            nearbyFood = "Coastal Fishermen Thali"
        ),
        TempleLookupPlace(
            name = "Konark Sun Temple",
            latitude = 19.8876,
            longitude = 86.0945,
            cityState = "Konark, Odisha",
            deity = "Surya Dev (Sun God)",
            architecture = "Kalinga Chariot Architecture",
            nearbyFood = "Odia Chenna Poda"
        ),
        TempleLookupPlace(
            name = "Sri Padmanabhaswamy Temple",
            latitude = 8.4828,
            longitude = 76.9436,
            cityState = "Thiruvananthapuram, Kerala",
            deity = "Lord Anantha Padmanabha (Vishnu)",
            architecture = "Kerala & Dravidian Fusion",
            nearbyFood = "Kerala Sadya, Boli & Payasam"
        ),
        TempleLookupPlace(
            name = "Mahakaleshwar Temple",
            latitude = 23.1827,
            longitude = 75.7682,
            cityState = "Ujjain, Madhya Pradesh",
            deity = "Lord Shiva (Dakshinamurti Jyotirlinga)",
            architecture = "Maratha & Bhumija Nagara",
            nearbyFood = "Ujjain Poha Jalebi, Rabdi"
        ),
        TempleLookupPlace(
            name = "Virupaksha Temple",
            latitude = 15.3350,
            longitude = 76.4600,
            cityState = "Hampi, Karnataka",
            deity = "Lord Shiva (Virupaksha)",
            architecture = "Vijayanagara Dravidian",
            nearbyFood = "Hampi Mango Tree Thali, Ragi Mudde"
        ),
        TempleLookupPlace(
            name = "Chennakeshava Temple",
            latitude = 13.1624,
            longitude = 75.8596,
            cityState = "Belur, Karnataka",
            deity = "Lord Chennakeshava (Vishnu)",
            architecture = "Hoysala Star-Shaped Soapstone",
            nearbyFood = "Malnad Benne Dosa"
        ),
        TempleLookupPlace(
            name = "Srirangam Ranganathaswamy Temple",
            latitude = 10.8624,
            longitude = 78.6901,
            cityState = "Tiruchirappalli, Tamil Nadu",
            deity = "Lord Ranganatha (Reclining Vishnu)",
            architecture = "Dravidian 21 Gopurams Island Temple",
            nearbyFood = "Srirangam Puliyodharai, Halwa"
        ),
        TempleLookupPlace(
            name = "Vaishno Devi Temple",
            latitude = 33.0308,
            longitude = 74.9490,
            cityState = "Katra, Jammu & Kashmir",
            deity = "Mata Vaishno Devi (Mahakali, Mahalakshmi, Mahasaraswati)",
            architecture = "Holy Trikuta Mountain Cave",
            nearbyFood = "Katra Rajma Chawal, Dry Fruits"
        ),
        TempleLookupPlace(
            name = "Kamakhya Temple",
            latitude = 26.1664,
            longitude = 91.7054,
            cityState = "Guwahati, Assam",
            deity = "Maa Kamakhya (Shakti Peeth)",
            architecture = "Nilachal Beehive Hybrid Style",
            nearbyFood = "Assamese Kheer, Pitha"
        ),
        TempleLookupPlace(
            name = "Angkor Wat",
            latitude = 13.4125,
            longitude = 103.8670,
            cityState = "Siem Reap, Cambodia",
            deity = "Lord Vishnu / Mount Meru",
            architecture = "Khmer Temple Mountain",
            nearbyFood = "Cambodian Coconut Curry"
        ),
        TempleLookupPlace(
            name = "Pashupatinath Temple",
            latitude = 27.7104,
            longitude = 85.3487,
            cityState = "Kathmandu, Nepal",
            deity = "Lord Pashupatinath (Shiva)",
            architecture = "Nepalese Pagoda Tiered Style",
            nearbyFood = "Newari Thali, Sel Roti"
        )
    )

    fun searchTemples(query: String): List<TempleLookupPlace> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return FAMOUS_TEMPLES.take(6)
        return FAMOUS_TEMPLES.filter {
            it.name.lowercase().contains(q) ||
            it.cityState.lowercase().contains(q) ||
            it.deity.lowercase().contains(q)
        }
    }
}
