package com.nexuserp.procurement.domain.model;

import com.nexuserp.core.domain.value.TenantId;

import java.util.UUID;

public class Supplier {

    public enum SupplierStatus { ACTIVE, INACTIVE, BLACKLISTED }

    private final UUID id;
    private final TenantId tenantId;
    private String code;
    private String name;
    private String contactName;
    private String email;
    private String phone;
    private String address;
    private String country;
    private String vatNumber;
    private String iban;
    private SupplierStatus status;
    private int paymentTermsDays;

    private Supplier(Builder b) {
        this.id = b.id != null ? b.id : UUID.randomUUID();
        this.tenantId = TenantId.of(b.tenantId);
        this.code = b.code;
        this.name = b.name;
        this.contactName = b.contactName;
        this.email = b.email;
        this.phone = b.phone;
        this.address = b.address;
        this.country = b.country;
        this.vatNumber = b.vatNumber;
        this.iban = b.iban;
        this.status = SupplierStatus.ACTIVE;
        this.paymentTermsDays = b.paymentTermsDays > 0 ? b.paymentTermsDays : 30;
    }

    public void deactivate() { this.status = SupplierStatus.INACTIVE; }
    public void blacklist() { this.status = SupplierStatus.BLACKLISTED; }

    public UUID getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getContactName() { return contactName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getCountry() { return country; }
    public String getVatNumber() { return vatNumber; }
    public String getIban() { return iban; }
    public SupplierStatus getStatus() { return status; }
    public int getPaymentTermsDays() { return paymentTermsDays; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id; private String tenantId, code, name, contactName;
        private String email, phone, address, country, vatNumber, iban;
        private int paymentTermsDays = 30;

        public Builder id(UUID i) { this.id = i; return this; }
        public Builder tenantId(String t) { this.tenantId = t; return this; }
        public Builder code(String c) { this.code = c; return this; }
        public Builder name(String n) { this.name = n; return this; }
        public Builder contactName(String c) { this.contactName = c; return this; }
        public Builder email(String e) { this.email = e; return this; }
        public Builder phone(String p) { this.phone = p; return this; }
        public Builder address(String a) { this.address = a; return this; }
        public Builder country(String c) { this.country = c; return this; }
        public Builder vatNumber(String v) { this.vatNumber = v; return this; }
        public Builder iban(String i) { this.iban = i; return this; }
        public Builder paymentTermsDays(int d) { this.paymentTermsDays = d; return this; }

        public Supplier build() {
            if (tenantId == null) throw new IllegalStateException("tenantId required");
            if (name == null) throw new IllegalStateException("name required");
            return new Supplier(this);
        }
    }
}
