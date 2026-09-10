package com.indieestate.backend.config

import com.indieestate.backend.dto.FormField
import com.indieestate.backend.dto.FormSchema
import com.indieestate.backend.entity.CategoryEntity
import com.indieestate.backend.entity.FormEntity
import com.indieestate.backend.entity.ServiceEntity
import com.indieestate.backend.repository.CategoryRepository
import com.indieestate.backend.repository.FormRepository
import com.indieestate.backend.repository.ServiceRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CatalogDataInitializer(
    private val categoryRepository: CategoryRepository,
    private val serviceRepository: ServiceRepository,
    private val formRepository: FormRepository,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        if (categoryRepository.count() > 0) return

        val jobs = category("0a1c0001-0000-4000-8000-000000000001", "jobs", "Jobs", "work", 1)
        val cars = category("0a1c0001-0000-4000-8000-000000000002", "cars", "Cars", "directions_car", 2)
        val bikes = category("0a1c0001-0000-4000-8000-000000000003", "bikes", "Bikes", "two_wheeler", 3)
        val properties = category("0a1c0001-0000-4000-8000-000000000004", "properties", "Properties", "home", 4)
        categoryRepository.saveAll(listOf(jobs, cars, bikes, properties))
        categoryRepository.saveAll(
            listOf(
                category("0a1c0001-0000-4000-8000-000000000011", "data-entry", "Data entry", "keyboard", 1, jobs.id),
                category("0a1c0001-0000-4000-8000-000000000012", "teacher", "Teacher", "school", 2, jobs.id),
                category("0a1c0001-0000-4000-8000-000000000013", "it", "IT", "computer", 3, jobs.id),
                category("0a1c0001-0000-4000-8000-000000000014", "salesman", "Salesman", "handshake", 4, jobs.id),
                category("0a1c0001-0000-4000-8000-000000000021", "sedan", "Sedan", "directions_car", 1, cars.id),
                category("0a1c0001-0000-4000-8000-000000000022", "suv", "SUV", "directions_car", 2, cars.id),
                category("0a1c0001-0000-4000-8000-000000000023", "hatchback", "Hatchback", "directions_car", 3, cars.id),
                category("0a1c0001-0000-4000-8000-000000000024", "luxury-car", "Luxury", "directions_car", 4, cars.id),
                category("0a1c0001-0000-4000-8000-000000000031", "motorcycle", "Motorcycle", "two_wheeler", 1, bikes.id),
                category("0a1c0001-0000-4000-8000-000000000032", "scooter", "Scooter", "two_wheeler", 2, bikes.id),
                category("0a1c0001-0000-4000-8000-000000000033", "electric-bike", "Electric", "two_wheeler", 3, bikes.id),
                category("0a1c0001-0000-4000-8000-000000000034", "sports-bike", "Sports", "two_wheeler", 4, bikes.id),
                category("0a1c0001-0000-4000-8000-000000000041", "apartment", "Apartment", "apartment", 1, properties.id),
                category("0a1c0001-0000-4000-8000-000000000042", "villa", "Villa", "villa", 2, properties.id),
                category("0a1c0001-0000-4000-8000-000000000043", "plot", "Plot", "map", 3, properties.id),
                category("0a1c0001-0000-4000-8000-000000000044", "commercial", "Commercial", "store", 4, properties.id),
            ),
        )

        val salon = service("0a1c0002-0000-4000-8000-000000000002", "salon", "Salon", "content_cut", 1)
        val toilet = service("0a1c0002-0000-4000-8000-000000000005", "toilet", "Toilet", "wc", 2)
        val pets = service("0a1c0002-0000-4000-8000-000000000006", "pets", "Pets", "pets", 3)
        val carWash = service("0a1c0002-0000-4000-8000-000000000007", "car-wash", "Car wash", "local_car_wash", 4)
        serviceRepository.saveAll(listOf(salon, toilet, pets, carWash))
        serviceRepository.saveAll(
            listOf(
                service("0a1c0002-0000-4000-8000-000000000011", "salon-men", "Men", "content_cut", 1, salon.id),
                service("0a1c0002-0000-4000-8000-000000000012", "salon-women", "Women", "content_cut", 2, salon.id),
                service("0a1c0002-0000-4000-8000-000000000013", "salon-unisex", "Unisex", "content_cut", 3, salon.id),
                service("0a1c0002-0000-4000-8000-000000000014", "salon-kids", "Kids", "content_cut", 4, salon.id),
                service("0a1c0002-0000-4000-8000-000000000021", "toilet-cleaning", "Cleaning", "wc", 1, toilet.id),
                service("0a1c0002-0000-4000-8000-000000000022", "toilet-repair", "Repair", "build", 2, toilet.id),
                service("0a1c0002-0000-4000-8000-000000000023", "toilet-installation", "Installation", "plumbing", 3, toilet.id),
                service("0a1c0002-0000-4000-8000-000000000031", "pets-grooming", "Grooming", "pets", 1, pets.id),
                service("0a1c0002-0000-4000-8000-000000000032", "pets-walking", "Walking", "pets", 2, pets.id),
                service("0a1c0002-0000-4000-8000-000000000033", "pets-boarding", "Boarding", "pets", 3, pets.id),
                service("0a1c0002-0000-4000-8000-000000000034", "pets-vet", "Vet", "local_hospital", 4, pets.id),
                service("0a1c0002-0000-4000-8000-000000000041", "car-wash-hatchback", "Hatchback", "local_car_wash", 1, carWash.id),
                service("0a1c0002-0000-4000-8000-000000000042", "car-wash-sedan", "Sedan", "local_car_wash", 2, carWash.id),
                service("0a1c0002-0000-4000-8000-000000000043", "car-wash-suv", "SUV", "local_car_wash", 3, carWash.id),
                service("0a1c0002-0000-4000-8000-000000000044", "car-wash-bike", "Bike wash", "two_wheeler", 4, carWash.id),
            ),
        )

        formRepository.save(
            FormEntity(
                id = UUID.fromString("0a1c0003-0000-4000-8000-000000000002"),
                code = "CAR_SELL",
                type = "SELL",
                categoryId = cars.id,
                title = "Sell a car",
                schema = FormSchema(
                    fields = listOf(
                        FormField("title", "Ad title", "text", required = true, maxLength = 80),
                        FormField("price", "Price", "number", required = true, min = 0.0),
                        FormField("brand", "Brand", "select", required = true, options = listOf("Maruti", "Hyundai", "Honda", "Tata", "Mahindra", "Toyota", "Other")),
                        FormField("model", "Model", "text", required = true, maxLength = 80),
                        FormField("year", "Year", "number", required = true, min = 1990.0, max = 2030.0),
                        FormField("kmDriven", "KM driven", "number", required = true, min = 0.0),
                        FormField("fuel", "Fuel", "select", required = true, options = listOf("Petrol", "Diesel", "CNG", "Electric", "Hybrid")),
                        FormField("description", "Description", "textarea", required = false, maxLength = 2000),
                    ),
                ),
            ),
        )
    }

    private fun category(
        id: String,
        slug: String,
        name: String,
        icon: String,
        order: Int,
        parentId: UUID? = null,
    ) = CategoryEntity(
        id = UUID.fromString(id),
        slug = slug,
        name = name,
        icon = icon,
        parentId = parentId,
        sortOrder = order,
    )

    private fun service(
        id: String,
        slug: String,
        name: String,
        icon: String,
        order: Int,
        parentId: UUID? = null,
    ) = ServiceEntity(
        id = UUID.fromString(id),
        slug = slug,
        name = name,
        icon = icon,
        parentId = parentId,
        sortOrder = order,
    )
}
