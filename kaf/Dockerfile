# Use an Alpine Linux base image with Go
FROM golang:alpine

# Install Git and other dependencies
RUN apk add --no-cache git

# Install kaf using go install
RUN go install github.com/birdayz/kaf/cmd/kaf@latest

# Set the working directory
WORKDIR /app

# Copy the kaf binary to the working directory
RUN cp /go/bin/kaf /app

# copy the config file to the working directory
RUN mkdir /root/.kaf/
COPY config /root/.kaf/config

# Set the entry point for the container
ENTRYPOINT ["/app/kaf"]

# Default command if no command is provided
CMD ["--help"]
