public final class Data extends PatternSet implements DataProducer {

    private final Collection<Mapper> mapperWrapper = new ArrayList<>();
    private File src;
    private String type;
    private Boolean conffile;
    private String destinationName;
    private MissingSourceBehavior missingSrc = FAIL;

    // getters and setters

    public void produce(final DataConsumer pReceiver) throws IOException {
        if (src == null || !src.exists()) {
            if (missingSrc == IGNORE) {
                return;
            } else {
                throw new FileNotFoundException("Data source not found : " + src);
            }
        }

        Mapper[] mappers = mapperWrapper.stream()
                .map(Mapper::createMapper)
                .toArray(Mapper[]::new);

        switch (type.toLowerCase()) {
            case "file":
                new DataProducerFile(src, destinationName, getIncludePatterns(getProject()), getExcludePatterns(getProject()), mappers).produce(pReceiver);
                break;
            case "archive":
                new DataProducerArchive(src, getIncludePatterns(getProject()), getExcludePatterns(getProject()), mappers).produce(pReceiver);
                break;
            case "directory":
                new DataProducerDirectory(src, getIncludePatterns(getProject()), getExcludePatterns(getProject()), mappers).produce(pReceiver);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported type: " + type);
        }
    }
}
