package com.indieestate.backend.config

import com.indieestate.backend.dto.FormField
import com.indieestate.backend.dto.FormSchema
import com.indieestate.backend.entity.FormEntity
import com.indieestate.backend.repository.FormRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Ensures built-in form schemas exist. Inserts only when [FormEntity.code] is missing —
 * never updates or deletes rows, so restarts and custom forms in Postgres are safe.
 */
@Component
@Order(20)
class DefaultFormsInitializer(
    private val formRepository: FormRepository,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        var inserted = 0
        defaultForms().forEach { template ->
            if (formRepository.findByCodeIgnoreCaseAndIsActiveTrue(template.code) != null) return@forEach
            formRepository.save(template.toEntity())
            inserted++
            log.info("Seeded default form: {}", template.code)
        }
        if (inserted > 0) {
            log.info("Default forms: inserted {} missing schema(s)", inserted)
        }
    }

    private data class FormTemplate(
        val id: UUID,
        val code: String,
        val type: String,
        val categoryId: UUID? = null,
        val serviceId: UUID? = null,
        val title: String,
        val schema: FormSchema,
    ) {
        fun toEntity() = FormEntity(
            id = id,
            code = code,
            type = type,
            categoryId = categoryId,
            serviceId = serviceId,
            title = title,
            schema = schema,
        )
    }

    private fun defaultForms(): List<FormTemplate> {
        val jobs = uuid("0a1c0001-0000-4000-8000-000000000001")
        val cars = uuid("0a1c0001-0000-4000-8000-000000000002")
        val bikes = uuid("0a1c0001-0000-4000-8000-000000000003")
        val properties = uuid("0a1c0001-0000-4000-8000-000000000004")
        val dataEntry = uuid("0a1c0001-0000-4000-8000-000000000011")
        val salon = uuid("0a1c0002-0000-4000-8000-000000000002")
        val toilet = uuid("0a1c0002-0000-4000-8000-000000000005")
        val pets = uuid("0a1c0002-0000-4000-8000-000000000006")
        val carWash = uuid("0a1c0002-0000-4000-8000-000000000007")

        return listOf(
            FormTemplate(
                id = uuid("0a1c0003-0000-4000-8000-000000000001"),
                code = "JOB_POST",
                type = "JOB",
                categoryId = jobs,
                title = "Post a job",
                schema = FormSchema(
                    fields = listOf(
                        FormField("jobTitle", "Job title", "text", required = true, maxLength = 80),
                        FormField("company", "Company", "text", required = true, maxLength = 80),
                        FormField("jobType", "Job type", "select", required = true, options = listOf("Full-time", "Part-time", "Contract", "Internship")),
                        FormField("salary", "Salary", "number", required = false, min = 0.0),
                        FormField("location", "Location", "text", required = true, maxLength = 120),
                        FormField("description", "Description", "textarea", required = true, maxLength = 2000),
                    ),
                ),
            ),
            FormTemplate(
                id = uuid("0a1c0003-0000-4000-8000-000000000010"),
                code = "DATA_ENTRY_JOB",
                type = "JOB",
                categoryId = dataEntry,
                title = "Post a data entry job",
                schema = FormSchema(
                    fields = listOf(
                        FormField("jobTitle", "Job title", "text", required = true, maxLength = 80),
                        FormField("company", "Company", "text", required = true, maxLength = 80),
                        FormField("jobType", "Job type", "select", required = true, options = listOf("Full-time", "Part-time", "Contract", "Work from home")),
                        FormField("salary", "Salary", "number", required = false, min = 0.0),
                        FormField("location", "Location", "text", required = true, maxLength = 120),
                        FormField("skills", "Skills", "text", required = false, maxLength = 200),
                        FormField("description", "Description", "textarea", required = true, maxLength = 2000),
                    ),
                ),
            ),
            FormTemplate(
                id = uuid("0a1c0003-0000-4000-8000-000000000002"),
                code = "CAR_SELL",
                type = "SELL",
                categoryId = cars,
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
            FormTemplate(
                id = uuid("0a1c0003-0000-4000-8000-000000000003"),
                code = "BIKE_SELL",
                type = "SELL",
                categoryId = bikes,
                title = "Sell a bike",
                schema = FormSchema(
                    fields = listOf(
                        FormField("title", "Ad title", "text", required = true, maxLength = 80),
                        FormField("price", "Price", "number", required = true, min = 0.0),
                        FormField("brand", "Brand", "select", required = true, options = listOf("Hero", "Honda", "Bajaj", "TVS", "Royal Enfield", "Yamaha", "Other")),
                        FormField("year", "Year", "number", required = true, min = 1990.0, max = 2030.0),
                        FormField("kmDriven", "KM driven", "number", required = true, min = 0.0),
                        FormField("description", "Description", "textarea", required = false, maxLength = 2000),
                    ),
                ),
            ),
            FormTemplate(
                id = uuid("0a1c0003-0000-4000-8000-000000000004"),
                code = "PROPERTY_SELL",
                type = "SELL",
                categoryId = properties,
                title = "Sell a property",
                schema = FormSchema(
                    fields = listOf(
                        FormField("title", "Ad title", "text", required = true, maxLength = 80),
                        FormField("price", "Price", "number", required = true, min = 0.0),
                        FormField("propertyType", "Property type", "select", required = true, options = listOf("Apartment", "Independent house", "Villa", "Plot", "Commercial")),
                        FormField("bedrooms", "Bedrooms", "number", required = false, min = 0.0),
                        FormField("bathrooms", "Bathrooms", "number", required = false, min = 0.0),
                        FormField("areaSqft", "Area (sqft)", "number", required = true, min = 1.0),
                        FormField("furnished", "Furnished", "boolean", required = false),
                        FormField("location", "Location", "text", required = true, maxLength = 160),
                        FormField("description", "Description", "textarea", required = false, maxLength = 2000),
                    ),
                ),
            ),
            FormTemplate(
                id = uuid("0a1c0003-0000-4000-8000-000000000101"),
                code = "SALON",
                type = "SERVICE_REQUEST",
                serviceId = salon,
                title = "Book a salon service",
                schema = serviceRequestSchema(
                    serviceOptions = listOf("Haircut", "Hair color", "Facial", "Spa", "Other"),
                ),
            ),
            FormTemplate(
                id = uuid("0a1c0003-0000-4000-8000-000000000102"),
                code = "TOILET_SERVICE",
                type = "SERVICE_REQUEST",
                serviceId = toilet,
                title = "Toilet service request",
                schema = serviceRequestSchema(
                    serviceOptions = listOf("Cleaning", "Repair", "Installation", "Other"),
                ),
            ),
            FormTemplate(
                id = uuid("0a1c0003-0000-4000-8000-000000000103"),
                code = "PETS_SERVICE",
                type = "SERVICE_REQUEST",
                serviceId = pets,
                title = "Pet service request",
                schema = FormSchema(
                    fields = listOf(
                        FormField("title", "Request title", "text", required = true, maxLength = 80),
                        FormField("serviceType", "Service type", "select", required = true, options = listOf("Grooming", "Walking", "Boarding", "Vet", "Other")),
                        FormField("petType", "Pet type", "select", required = true, options = listOf("Dog", "Cat", "Bird", "Other")),
                        FormField("preferredDate", "Preferred date", "date", required = true),
                        FormField("address", "Address", "textarea", required = true, maxLength = 300),
                        FormField("notes", "Notes", "textarea", required = false, maxLength = 1000),
                    ),
                ),
            ),
            FormTemplate(
                id = uuid("0a1c0003-0000-4000-8000-000000000104"),
                code = "CAR_WASH_SERVICE",
                type = "SERVICE_REQUEST",
                serviceId = carWash,
                title = "Car wash request",
                schema = FormSchema(
                    fields = listOf(
                        FormField("title", "Request title", "text", required = true, maxLength = 80),
                        FormField("vehicleType", "Vehicle type", "select", required = true, options = listOf("Hatchback", "Sedan", "SUV", "Bike", "Other")),
                        FormField("preferredDate", "Preferred date", "date", required = true),
                        FormField("address", "Address", "textarea", required = true, maxLength = 300),
                        FormField("notes", "Notes", "textarea", required = false, maxLength = 1000),
                    ),
                ),
            ),
        )
    }

    private fun serviceRequestSchema(serviceOptions: List<String>) = FormSchema(
        fields = listOf(
            FormField("title", "Request title", "text", required = true, maxLength = 80),
            FormField("serviceType", "Service type", "select", required = true, options = serviceOptions),
            FormField("preferredDate", "Preferred date", "date", required = true),
            FormField("address", "Address", "textarea", required = true, maxLength = 300),
            FormField("notes", "Notes", "textarea", required = false, maxLength = 1000),
        ),
    )

    private fun uuid(text: String) = UUID.fromString(text)
}
