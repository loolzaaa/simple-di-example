package ru.loolzaaa.teach.simpledi.context;

import ru.loolzaaa.teach.simpledi.annotations.Element;
import ru.loolzaaa.teach.simpledi.annotations.ElementScan;
import ru.loolzaaa.teach.simpledi.annotations.ElementWire;
import ru.loolzaaa.teach.simpledi.annotations.PrimaryElement;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

public class Context {

    // Список для хранения классов-кандидатов для создания бинов
    private List<Class<?>> elementClasses = new LinkedList<>();

    // Упрощенная верчия IoC контейнера, где ключом является имя бина, а значением сам бин.
    // Далее - контейнер
    private Map<String, Object> container = new HashMap<>();

    // При создании экземпляра контекста ему передается класс, аннотация которого указывает
    // на базовый пакет для сканирования классов-кандидатов
    public Context(Class<?> clazz) throws ReflectiveOperationException {
        init(clazz);
    }

    private void init(Class<?> clazz) throws ReflectiveOperationException {
        // Проверка наличия аннотации, указывающей на базовый пакет для сканирования
        boolean isNeedToScan = clazz.isAnnotationPresent(ElementScan.class);
        if (isNeedToScan) {
            // Получение расположения базового пакета для сканирования в classpath
            String path = clazz.getAnnotation(ElementScan.class).value();
            System.out.println("Scanning in classpath:" + path);

            // Преобразование представления пакетного пути
            String scanPath = clazz.getResource("/" + path.replaceAll("\\.", "/")).getPath();

            // Наполнение списка для хранения классов-кандидатов
            scanForElements(path, scanPath);

            // Первая итерация по списку классов-кандидатов рассматривает те классы, где присутствует
            // только конструктор по умолчанию.
            // Это позволяет создать те бины, которые не имеют зависимостей и поместить их в контейнер
            Iterator<Class<?>> iterator = elementClasses.iterator();
            while (iterator.hasNext()) {
                Class<?> cls = iterator.next();
                if (cls.getConstructors().length > 1) continue;
                try {
                    createAndAddBeanToContainer(cls, cls.getConstructor(), null);
                    iterator.remove();
                } catch (NoSuchMethodException ignored) {
                    // Данное исключение будет выброшено в том случае если единственный конструктор
                    // не является конструктором по умолчанию
                    // (см. метод Constructor<T> getConstructor(Class<?>... parameterTypes))
                }
            }

            // Вторая итерация по списку классов-кандидатов рассматривает все оставшиеся классы,
            // независимо от количества конструкторов после сортировки их по зависимости друг от друга.
            elementClasses.sort((o1, o2) -> {
                Constructor<?> o1Constructor = getValidConstructor(o1);
                //Теоретически невозможная ситуация т.к. лист содержит сами классы.
                if (o1.equals(o2)) return 0;
                if (Arrays.stream(o1Constructor.getParameterTypes()).anyMatch(aClass -> aClass.equals(o2))) {
                    return 1;
                } else {
                    return -1;
                }
            });
            iterator = elementClasses.iterator();
            while (iterator.hasNext()) {
                Class<?> cls = iterator.next();
                // При наличии валидного конструктора в классе-кандидате,
                // сперва необходимо получить для него бины-зависимости из контейнера,
                // которые будут внедрены в конструктор при помощи рефлексии
                // с целью создания нового бина с зависимостями
                Constructor<?> constructor = getValidConstructor(cls);
                List<Object> parameters = getConstructorDependencies(constructor);
                createAndAddBeanToContainer(cls, constructor, parameters);
                iterator.remove();
            }
            assert elementClasses.size() == 0;
        }
    }

    // Метод рекурсивного сканирования определенной директории с учетом вложенных папок с целью поиска class-файлов,
    // у которых имеется аннотация Element, явно указывающая на то, что данный класс является классом-кандидатом
    // для создания бина
    private void scanForElements(String classPath, String scanPath) throws ClassNotFoundException {
        File[] files = new File(scanPath).listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".class")) {
                String className = f.getName().replaceAll("\\.class$", "");
                Class<?> cls = Class.forName(classPath + "." + className);
                if (cls.isAnnotationPresent(Element.class)) {
                    System.out.println("Found element: " + className);
                    elementClasses.add(cls);
                }
            } else {
                scanForElements(classPath + "." + f.getName(), f.getPath());
            }
        }
    }

    // Метод получения бинов из контейнера, которые являются зависимостями передаваемого в данный метод конструктора
    private List<Object> getConstructorDependencies(Constructor<?> constructor) {
        List<Object> parameters = new LinkedList<>();
        // Последовательный перебор классов аргументов переданного конструктора
        for (Class<?> c : constructor.getParameterTypes()) {
            String name = getBeanClassName(c);
            // В первую очередь происходит поиск бина в контейнере по имени класса аргумента переданного конструктора
            Object bean = container.get(name);
            if (bean != null) {
                // Если бин получен из контейнера по имени, то можно сразу добавить его в список завивимостей
                parameters.add(bean);
            } else {
                // В тех случаях, когда в контейнере отсутствует бин с именем класса аргумента переданного конструктора,
                // производится поиск всех бинов в контейнере,
                // которые подходят по типу аргумента переданного конструктора
                List<Object> validBeans = container.values().stream()
                        .filter(obj -> c.isAssignableFrom(obj.getClass()))
                        .collect(Collectors.toList());
                if (validBeans.size() == 1) {
                    // Если в контейнере найден бин в единственном экземпляре,
                    // то можно сразу добавить его в список завивимостей
                    parameters.add(validBeans.get(0));
                } else if (validBeans.size() > 1) {
                    // В тех случаях, когда в контейнере найдено более одного бина,
                    // производится поиск первичной аннотации PrimaryElement, которая явно указывает на тот бин,
                    // который следует использовать в неоднозначных ситуациях
                    validBeans = validBeans.stream()
                            .filter(o -> o.getClass().isAnnotationPresent(PrimaryElement.class))
                            .collect(Collectors.toList());
                    if (validBeans.size() == 1) {
                        // Если первичная аннотация указана на единственном бине,
                        // то можно сразу добавить его в список завивимостей
                        parameters.add(validBeans.get(0));
                    } else {
                        // Если первичная аннотация отсутствует или имеется более чем на одном бине,
                        // то выбрасывается исключение, явно указывающее на то,
                        // что контейнере имеется более одного бина, подходящего под зависимость
                        throw new IllegalArgumentException("To much valid beans in container for " + name + " dependency");
                    }
                } else {
                    // В тех случаях, когда в контейнере не найдено подходящих бинов,
                    // выбрасывается соответствющее исключение
                    throw new NoSuchElementException("There is no such bean in context container! Bean name: " + name);
                }
            }
        }
        return parameters;
    }

    // Метод для создания бина с опциональным внедрением соответсвующих зависимостей,
    // а также последующим помещением бина в контейнер
    private void createAndAddBeanToContainer(Class<?> clazz, Constructor<?> constructor, List<Object> parameters)
            throws ReflectiveOperationException {
        Object o;
        if (parameters == null) {
            o = constructor.newInstance();
        } else {
            o = constructor.newInstance(parameters.toArray());
        }
        String name = getBeanClassName(clazz);
        container.put(name, o);
        System.out.println("Bean created: " + name);
    }

    private String getBeanClassName(Class<?> clazz) {
        return clazz.getSimpleName().substring(0, 1).toLowerCase() + clazz.getSimpleName().substring(1);
    }

    private Constructor<?> getValidConstructor(Class<?> clazz) {
        Constructor<?> clazzConstructor;
        if (clazz.getConstructors().length == 1) {
            // При наличии единственного конструктора в классе-кандидате,
            // сперва необходимо получить для него бины-зависимости из контейнера,
            // которые будут внедрены в конструктор при помощи рефлексии
            // с целью создания нового бина с зависимостями
            clazzConstructor = clazz.getConstructors()[0];
        } else {
            // В тех случаях, когда имеется более одного конструктора
            // производится поиск аннотации связывания ElementWire,
            // которая явно указывает конструктор для создания бина
            List<Constructor<?>> validConstructors = Arrays.stream(clazz.getConstructors())
                    .filter(c -> c.isAnnotationPresent(ElementWire.class))
                    .collect(Collectors.toList());
            if (validConstructors.size() == 1) {
                // Если аннотация представлена на единственном конструкторе,
                // то сперва необходимо получить для него бины-зависимости из контейнера,
                // которые будут внедрены в конструктор при помощи рефлексии
                // с целью создания нового бина с зависимостями
                clazzConstructor = validConstructors.get(0);
            } else if (validConstructors.size() == 0) {
                // В тех случаях, когда аннотация связывания отсутствует при наличии двух и более конструкторов
                // выбрасывается соответствующее исключение,
                // которое указывает на неоднозначночть выбора конструктора для связывания
                throw new IllegalArgumentException("There is more than one constructor in class " + clazz +
                        ", but ElementWire annotation did not found.");
            } else {
                // Аналогично, ошибкой является наличие аннотации связывания более чем у одного конструктора,
                // что также указывает на неоднозначность выбора конструктора для связывания
                throw new IllegalArgumentException("There is more than one constructor in class " + clazz +
                        " with ElementWire annotation.");
            }
        }
        return clazzConstructor;
    }

    // Получение бина из контейнера контекста
    public Object getBean(String name) {
        return container.get(name);
    }
}
