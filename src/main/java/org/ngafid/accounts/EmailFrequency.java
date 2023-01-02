package org.ngafid.accounts;

public enum EmailFrequency {
        NONE("NONE"),
        DAILY("DAILY"),
        WEEKLY("WEEKLY"),
        MONTHLY("MONTHLY"),
        QUARTERLY("QUARTERLY"),
        YEARLY("YEARLY");

        private final String frequency;

        EmailFrequency(String freq) {
            this.frequency = freq;
        }

        @Override
        public String toString() {
            return this.frequency;
        }
}
