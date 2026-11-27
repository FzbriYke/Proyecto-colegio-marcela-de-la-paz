package cl.marcelapaz;

import java.util.List;
import java.util.function.Function;

interface ColeccionJson<T> {
    List<T> leer();
    <R> R modificar(Function<List<T>, R> cambio);
}
