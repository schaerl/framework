/*
 * Copyright 2000-2014 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.vaadin.data.util.converter.Converter;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.event.Registration;
import com.vaadin.server.ErrorMessage;
import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractComponent;

/**
 * Connects one or more {@code Field} components to properties of a backing data
 * type such as a bean type. With a binder, input components can be grouped
 * together into forms to easily create and update business objects with little
 * explicit logic needed to move data between the UI and the data layers of the
 * application.
 * <p>
 * A binder is a collection of <i>bindings</i>, each representing the
 * association of a single field and a backing property.
 * <p>
 * A binder instance can be bound to a single bean instance at a time, but can
 * be rebound as needed. This allows usage patterns like a <i>master-details</i>
 * view, where a select component is used to pick the bean to edit.
 * <p>
 * Unless otherwise specified, {@code Binder} method arguments cannot be null.
 *
 * @author Vaadin Ltd.
 *
 * @param <BEAN>
 *            the bean type
 *
 * @see Binding
 * @see HasValue
 *
 * @since
 */
public class Binder<BEAN> implements Serializable {

    /**
     * Represents the binding between a field and a data property.
     *
     * @param <BEAN>
     *            the bean type
     * @param <FIELDVALUE>
     *            the value type of the field
     * @param <TARGET>
     *            the target data type of the binding, matches the field type
     *            until a converter has been set
     *
     * @see Binder#forField(HasValue)
     */
    public interface Binding<BEAN, FIELDVALUE, TARGET> extends Serializable {

        /**
         * Completes this binding using the given getter and setter functions
         * representing a backing bean property. The functions are used to
         * update the field value from the property and to store the field value
         * to the property, respectively.
         * <p>
         * When a bean is bound with {@link Binder#bind(BEAN)}, the field value
         * is set to the return value of the given getter. The property value is
         * then updated via the given setter whenever the field value changes.
         * The setter may be null; in that case the property value is never
         * updated and the binding is said to be <i>read-only</i>.
         * <p>
         * If the Binder is already bound to some item, the newly bound field is
         * associated with the corresponding bean property as described above.
         * <p>
         * The getter and setter can be arbitrary functions, for instance
         * implementing user-defined conversion or validation. However, in the
         * most basic use case you can simply pass a pair of method references
         * to this method as follows:
         *
         * <pre>
         * class Person {
         *     public String getName() { ... }
         *     public void setName(String name) { ... }
         * }
         *
         * TextField nameField = new TextField();
         * binder.forField(nameField).bind(Person::getName, Person::setName);
         * </pre>
         *
         * @param getter
         *            the function to get the value of the property to the
         *            field, not null
         * @param setter
         *            the function to save the field value to the property or
         *            null if read-only
         * @throws IllegalStateException
         *             if {@code bind} has already been called on this binding
         */
        public void bind(Function<BEAN, TARGET> getter,
                BiConsumer<BEAN, TARGET> setter);

        /**
         * Adds a validator to this binding. Validators are applied, in
         * registration order, when the field value is saved to the backing
         * property. If any validator returns a failure, the property value is
         * not updated.
         *
         * @param validator
         *            the validator to add, not null
         * @return this binding, for chaining
         * @throws IllegalStateException
         *             if {@code bind} has already been called
         */
        public Binding<BEAN, FIELDVALUE, TARGET> withValidator(
                Validator<? super TARGET> validator);

        /**
         * A convenience method to add a validator to this binding using the
         * {@link Validator#from(Predicate, String)} factory method.
         * <p>
         * Validators are applied, in registration order, when the field value
         * is saved to the backing property. If any validator returns a failure,
         * the property value is not updated.
         *
         * @see #withValidator(Validator)
         * @see Validator#from(Predicate, String)
         *
         * @param predicate
         *            the predicate performing validation, not null
         * @param message
         *            the error message to report in case validation failure
         * @return this binding, for chaining
         * @throws IllegalStateException
         *             if {@code bind} has already been called
         */
        public Binding<BEAN, FIELDVALUE, TARGET> withValidator(
                Predicate<? super TARGET> predicate, String message);

        /**
         * Maps the binding to another data type using the given
         * {@link Converter}.
         * <p>
         * A converter is capable of converting between a presentation type,
         * which must match the current target data type of the binding, and a
         * model type, which can be any data type and becomes the new target
         * type of the binding. When invoking
         * {@link #bind(Function, BiConsumer)}, the target type of the binding
         * must match the getter/setter types.
         * <p>
         * For instance, a {@code TextField} can be bound to an integer-typed
         * property using an appropriate converter such as a
         * {@link StringToIntegerConverter}.
         *
         * @param <NEWTARGET>
         *            the type to convert to
         * @param converter
         *            the converter to use, not null
         * @return a new binding with the appropriate type
         * @throws IllegalStateException
         *             if {@code bind} has already been called
         */
        public <NEWTARGET> Binding<BEAN, FIELDVALUE, NEWTARGET> withConverter(
                Converter<TARGET, NEWTARGET> converter);

        /**
         * Maps the binding to another data type using the mapping functions and
         * a possible exception as the error message.
         * <p>
         * The mapping functions are used to convert between a presentation
         * type, which must match the current target data type of the binding,
         * and a model type, which can be any data type and becomes the new
         * target type of the binding. When invoking
         * {@link #bind(Function, BiConsumer)}, the target type of the binding
         * must match the getter/setter types.
         * <p>
         * For instance, a {@code TextField} can be bound to an integer-typed
         * property using appropriate functions such as:
         * <code>withConverter(Integer::valueOf, String::valueOf);</code>
         *
         * @param <NEWTARGET>
         *            the type to convert to
         * @param toModel
         *            the function which can convert from the old target type to
         *            the new target type
         * @param toPresentation
         *            the function which can convert from the new target type to
         *            the old target type
         * @return a new binding with the appropriate type
         * @throws IllegalStateException
         *             if {@code bind} has already been called
         */
        default public <NEWTARGET> Binding<BEAN, FIELDVALUE, NEWTARGET> withConverter(
                Function<TARGET, NEWTARGET> toModel,
                Function<NEWTARGET, TARGET> toPresentation) {
            return withConverter(Converter.from(toModel, toPresentation,
                    exception -> exception.getMessage()));
        }

        /**
         * Maps the binding to another data type using the mapping functions and
         * the given error error message if a value cannot be converted to the
         * new target type.
         * <p>
         * The mapping functions are used to convert between a presentation
         * type, which must match the current target data type of the binding,
         * and a model type, which can be any data type and becomes the new
         * target type of the binding. When invoking
         * {@link #bind(Function, BiConsumer)}, the target type of the binding
         * must match the getter/setter types.
         * <p>
         * For instance, a {@code TextField} can be bound to an integer-typed
         * property using appropriate functions such as:
         * <code>withConverter(Integer::valueOf, String::valueOf);</code>
         *
         * @param <NEWTARGET>
         *            the type to convert to
         * @param toModel
         *            the function which can convert from the old target type to
         *            the new target type
         * @param toPresentation
         *            the function which can convert from the new target type to
         *            the old target type
         * @param errorMessage
         *            the error message to use if conversion using
         *            <code>toModel</code> fails
         * @return a new binding with the appropriate type
         * @throws IllegalStateException
         *             if {@code bind} has already been called
         */
        public default <NEWTARGET> Binding<BEAN, FIELDVALUE, NEWTARGET> withConverter(
                Function<TARGET, NEWTARGET> toModel,
                Function<NEWTARGET, TARGET> toPresentation,
                String errorMessage) {
            return withConverter(Converter.from(toModel, toPresentation,
                    exception -> errorMessage));
        }

        /**
         * Gets the field the binding uses.
         *
         * @return the field for the binding
         */
        public HasValue<FIELDVALUE> getField();

    }

    /**
     * An internal implementation of {@code Binding}.
     *
     * @param <BEAN>
     *            the bean type, must match the Binder bean type
     * @param <FIELDVALUE>
     *            the value type of the field
     * @param <TARGET>
     *            the target data type of the binding, matches the field type
     *            until a converter has been set
     */
    protected static class BindingImpl<BEAN, FIELDVALUE, TARGET>
            implements Binding<BEAN, FIELDVALUE, TARGET> {

        private final Binder<BEAN> binder;

        private final HasValue<FIELDVALUE> field;
        private Registration onValueChange;

        private Function<BEAN, TARGET> getter;
        private BiConsumer<BEAN, TARGET> setter;

        /**
         * Contains all converters and validators chained together in the
         * correct order.
         */
        private Converter<FIELDVALUE, TARGET> converterValidatorChain;

        /**
         * Creates a new binding associated with the given field.
         *
         * @param binder
         *            the binder this instance is connected to
         * @param field
         *            the field to bind
         */
        @SuppressWarnings("unchecked")
        protected BindingImpl(Binder<BEAN> binder, HasValue<FIELDVALUE> field) {
            this(binder, field,
                    (Converter<FIELDVALUE, TARGET>) Converter.identity());
        }

        /**
         * Creates a new binding associated with the given field using the given
         * converter chain.
         *
         * @param binder
         *            the binder this instance is connected to
         * @param field
         *            the field to bind
         * @param converterValidatorChain
         *            the converter/validator chain to use
         */
        protected BindingImpl(Binder<BEAN> binder, HasValue<FIELDVALUE> field,
                Converter<FIELDVALUE, TARGET> converterValidatorChain) {
            this.field = field;
            this.binder = binder;
            this.converterValidatorChain = converterValidatorChain;
        }

        @Override
        public void bind(Function<BEAN, TARGET> getter,
                BiConsumer<BEAN, TARGET> setter) {
            checkUnbound();
            Objects.requireNonNull(getter, "getter cannot be null");

            this.getter = getter;
            this.setter = setter;
            binder.bindings.add(this);
            binder.getBean().ifPresent(this::bind);
        }

        @Override
        public Binding<BEAN, FIELDVALUE, TARGET> withValidator(
                Validator<? super TARGET> validator) {
            checkUnbound();
            Objects.requireNonNull(validator, "validator cannot be null");

            Converter<TARGET, TARGET> validatorAsConverter = new ValidatorAsConverter<>(
                    validator);
            converterValidatorChain = converterValidatorChain
                    .chain(validatorAsConverter);
            return this;
        }

        @Override
        public Binding<BEAN, FIELDVALUE, TARGET> withValidator(
                Predicate<? super TARGET> predicate, String message) {
            return withValidator(Validator.from(predicate, message));
        }

        @Override
        public <NEWTARGET> Binding<BEAN, FIELDVALUE, NEWTARGET> withConverter(
                Converter<TARGET, NEWTARGET> converter) {
            checkUnbound();
            Objects.requireNonNull(converter, "converter cannot be null");

            BindingImpl<BEAN, FIELDVALUE, NEWTARGET> newBinding = new BindingImpl<>(
                    binder, field, converterValidatorChain.chain(converter));
            return newBinding;
        }

        private void bind(BEAN bean) {
            setFieldValue(bean);
            onValueChange = field
                    .addValueChangeListener(e -> storeFieldValue(bean));
        }

        private Result<TARGET> validate() {
            FIELDVALUE fieldValue = field.getValue();
            Result<TARGET> dataValue = converterValidatorChain.convertToModel(
                    fieldValue, ((AbstractComponent) field).getLocale());
            return dataValue;
        }

        /**
         * Returns the field value run through all converters and validators.
         *
         * @return an optional containing the validated and converted value or
         *         an empty optional if a validator or converter failed
         */
        private Optional<TARGET> getTargetValue() {
            return validate().getValue();
        }

        private void unbind() {
            onValueChange.remove();
        }

        /**
         * Sets the field value by invoking the getter function on the given
         * bean.
         *
         * @param bean
         *            the bean to fetch the property value from
         */
        private void setFieldValue(BEAN bean) {
            assert bean != null;
            field.setValue(convertDataToFieldType(bean));
        }

        private FIELDVALUE convertDataToFieldType(BEAN bean) {
            return converterValidatorChain.convertToPresentation(
                    getter.apply(bean),
                    ((AbstractComponent) field).getLocale());
        }

        /**
         * Saves the field value by invoking the setter function on the given
         * bean, if the value passes all registered validators.
         *
         * @param bean
         *            the bean to set the property value to
         */
        private void storeFieldValue(BEAN bean) {
            assert bean != null;
            if (setter != null) {
                getTargetValue().ifPresent(value -> setter.accept(bean, value));
            }
        }

        private void checkUnbound() {
            if (getter != null) {
                throw new IllegalStateException(
                        "cannot modify binding: already bound to a property");
            }
        }

        @Override
        public HasValue<FIELDVALUE> getField() {
            return field;
        }
    }

    /**
     * Wraps a validator as a converter.
     * <p>
     * The type of the validator must be of the same type as this converter or a
     * super type of it.
     *
     * @param <T>
     *            the type of the converter
     */
    private static class ValidatorAsConverter<T> implements Converter<T, T> {

        private Validator<? super T> validator;

        /**
         * Creates a new converter wrapping the given validator.
         *
         * @param validator
         *            the validator to wrap
         */
        public ValidatorAsConverter(Validator<? super T> validator) {
            this.validator = validator;
        }

        @Override
        public Result<T> convertToModel(T value, Locale locale) {
            Result<? super T> validationResult = validator.apply(value);
            if (validationResult.isError()) {
                return Result.error(validationResult.getMessage().get());
            } else {
                return Result.ok(value);
            }
        }

        @Override
        public T convertToPresentation(T value, Locale locale) {
            return value;
        }

    }

    private BEAN bean;

    private Set<BindingImpl<BEAN, ?, ?>> bindings = new LinkedHashSet<>();

    /**
     * Returns an {@code Optional} of the bean that has been bound with
     * {@link #bind}, or an empty optional if a bean is not currently bound.
     *
     * @return the currently bound bean if any
     */
    public Optional<BEAN> getBean() {
        return Optional.ofNullable(bean);
    }

    /**
     * Creates a new binding for the given field. The returned binding may be
     * further configured before invoking
     * {@link Binding#bind(Function, BiConsumer) Binding.bind} which completes
     * the binding. Until {@code Binding.bind} is called, the binding has no
     * effect.
     *
     * @param <FIELDVALUE>
     *            the value type of the field
     * @param field
     *            the field to be bound, not null
     * @return the new binding
     */
    public <FIELDVALUE> Binding<BEAN, FIELDVALUE, FIELDVALUE> forField(
            HasValue<FIELDVALUE> field) {
        return createBinding(field);
    }

    /**
     * Binds a field to a bean property represented by the given getter and
     * setter pair. The functions are used to update the field value from the
     * property and to store the field value to the property, respectively.
     * <p>
     * Use the {@link #forField(HasValue)} overload instead if you want to
     * further configure the new binding.
     * <p>
     * When a bean is bound with {@link Binder#bind(BEAN)}, the field value is
     * set to the return value of the given getter. The property value is then
     * updated via the given setter whenever the field value changes. The setter
     * may be null; in that case the property value is never updated and the
     * binding is said to be <i>read-only</i>.
     * <p>
     * If the Binder is already bound to some item, the newly bound field is
     * associated with the corresponding bean property as described above.
     * <p>
     * The getter and setter can be arbitrary functions, for instance
     * implementing user-defined conversion or validation. However, in the most
     * basic use case you can simply pass a pair of method references to this
     * method as follows:
     *
     * <pre>
     * class Person {
     *     public String getName() { ... }
     *     public void setName(String name) { ... }
     * }
     *
     * TextField nameField = new TextField();
     * binder.bind(nameField, Person::getName, Person::setName);
     * </pre>
     *
     * @param <FIELDVALUE>
     *            the value type of the field
     * @param field
     *            the field to bind, not null
     * @param getter
     *            the function to get the value of the property to the field,
     *            not null
     * @param setter
     *            the function to save the field value to the property or null
     *            if read-only
     */
    public <FIELDVALUE> void bind(HasValue<FIELDVALUE> field,
            Function<BEAN, FIELDVALUE> getter,
            BiConsumer<BEAN, FIELDVALUE> setter) {
        forField(field).bind(getter, setter);
    }

    /**
     * Binds the given bean to all the fields added to this Binder. To remove
     * the binding, call {@link #unbind()}.
     * <p>
     * When a bean is bound, the field values are updated by invoking their
     * corresponding getter functions. Any changes to field values are reflected
     * back to their corresponding property values of the bean as long as the
     * bean is bound.
     *
     * @param bean
     *            the bean to edit, not null
     */
    public void bind(BEAN bean) {
        Objects.requireNonNull(bean, "bean cannot be null");
        unbind();
        this.bean = bean;
        bindings.forEach(b -> b.bind(bean));
    }

    /**
     * Validates the values of all bound fields and returns the result of the
     * validation as a set of validation errors.
     * <p>
     * Validation is successful if the resulting set is empty.
     *
     * @return the validation result.
     */
    public List<ValidationError<?>> validate() {
        List<ValidationError<?>> resultErrors = new ArrayList<>();
        for (BindingImpl<BEAN, ?, ?> binding : bindings) {
            clearError(binding.field);

            binding.validate().ifError(errorMessage -> {
                resultErrors.add(
                        new ValidationError<>(binding.field, errorMessage));
                handleError(binding.field, errorMessage);
            });
        }
        return resultErrors;
    }

    /**
     * Unbinds the currently bound bean if any. If there is no bound bean, does
     * nothing.
     */
    public void unbind() {
        if (bean != null) {
            bean = null;
            bindings.forEach(BindingImpl::unbind);
        }
    }

    /**
     * Reads the bound property values from the given bean to the corresponding
     * fields. The bean is not otherwise associated with this binder; in
     * particular its property values are not bound to the field value changes.
     * To achieve that, use {@link #bind(BEAN)}.
     *
     * @param bean
     *            the bean whose property values to read, not null
     */
    public void load(BEAN bean) {
        Objects.requireNonNull(bean, "bean cannot be null");
        bindings.forEach(binding -> binding.setFieldValue(bean));

    }

    /**
     * Saves changes from the bound fields to the edited bean. If any value
     * fails validation, no values are saved and an {@code BindingException} is
     * thrown.
     *
     * @param bean
     *            the object to which to save the field values, not null
     * @throws BindingException
     *             if some of the bound field values fail to validate
     */
    public void save(BEAN bean) {
        Objects.requireNonNull(bean, "bean cannot be null");
        bindings.forEach(binding -> binding.storeFieldValue(bean));
    }

    /**
     * Creates a new binding with the given field.
     *
     * @param <FIELDVALUE>
     *            the value type of the field
     * @param field
     *            the field to bind
     * @return the new incomplete binding
     */
    protected <FIELDVALUE> BindingImpl<BEAN, FIELDVALUE, FIELDVALUE> createBinding(
            HasValue<FIELDVALUE> field) {
        Objects.requireNonNull(field, "field cannot be null");
        BindingImpl<BEAN, FIELDVALUE, FIELDVALUE> b = new BindingImpl<BEAN, FIELDVALUE, FIELDVALUE>(
                this, field);
        return b;
    }

    /**
     * Clears the error condition of the given field, if any. The default
     * implementation clears the
     * {@link AbstractComponent#setComponentError(ErrorMessage) component error}
     * of the field if it is a Component, otherwise does nothing.
     *
     * @param field
     *            the field with an invalid value
     */
    protected void clearError(HasValue<?> field) {
        if (field instanceof AbstractComponent) {
            ((AbstractComponent) field).setComponentError(null);
        }
    }

    /**
     * Handles a validation error emitted when trying to save the value of the
     * given field. The default implementation sets the
     * {@link AbstractComponent#setComponentError(ErrorMessage) component error}
     * of the field if it is a Component, otherwise does nothing.
     *
     * @param field
     *            the field with the invalid value
     * @param error
     *            the error message to set
     */
    protected void handleError(HasValue<?> field, String error) {
        if (field instanceof AbstractComponent) {
            ((AbstractComponent) field).setComponentError(new UserError(error));
        }
    }

}