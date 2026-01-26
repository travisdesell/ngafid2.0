FROM python:3.10-slim
# Install system dependencies for GDAL
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        gdal-bin \
        libgdal-dev \
        build-essential \
        curl \
        lsof && \
    rm -rf /var/lib/apt/lists/*
# Set environment variable for GDAL
ENV CPLUS_INCLUDE_PATH=/usr/include/gdal
ENV C_INCLUDE_PATH=/usr/include/gdal
# Copy project files
WORKDIR /app
COPY . /app
# Install the package (editable mode for development)
RUN pip install --no-cache-dir -e .
# Expose default port
EXPOSE 8187
# Default command to run the server
CMD ["aviation-chart-server", "--host", "0.0.0.0", "--port", "8187"]
