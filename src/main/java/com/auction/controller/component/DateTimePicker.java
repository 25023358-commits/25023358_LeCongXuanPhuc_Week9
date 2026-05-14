package com.auction.controller.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.DatePicker;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DateTimePicker extends DatePicker {
    private final ObjectProperty<LocalDateTime> dateTimeValue = new SimpleObjectProperty<>(LocalDateTime.now());
    private final ObjectProperty<String> format = new SimpleObjectProperty<String>("yyyy-MM-dd HH:mm");

    public DateTimePicker() {
        super(LocalDate.now());
        setConverter(new javafx.util.StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate object) {
                return dateTimeValue.get().format(java.time.format.DateTimeFormatter.ofPattern(format.get()));
            }

            @Override
            public LocalDate fromString(String string) {
                dateTimeValue.set(LocalDateTime.parse(string, java.time.format.DateTimeFormatter.ofPattern(format.get())));
                return dateTimeValue.get().toLocalDate();
            }
        });

        valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (dateTimeValue.get() == null) {
                    dateTimeValue.set(LocalDateTime.of(newValue, LocalTime.now()));
                } else {
                    LocalTime time = dateTimeValue.get().toLocalTime();
                    dateTimeValue.set(LocalDateTime.of(newValue, time));
                }
            }
        });
    }

    public LocalDateTime getDateTimeValue() {
        return dateTimeValue.get();
    }

    public void setDateTimeValue(LocalDateTime dateTimeValue) {
        this.dateTimeValue.set(dateTimeValue);
    }

    public ObjectProperty<LocalDateTime> dateTimeValueProperty() {
        return dateTimeValue;
    }

    public String getFormat() {
        return format.get();
    }

    public void setFormat(String format) {
        this.format.set(format);
    }

    public ObjectProperty<String> formatProperty() {
        return format;
    }
}
