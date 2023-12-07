# Coursework Summary: Operating Systems and Concurrency - Assignment 1

## Digital Image Processor

This coursework, constituting 7.5% of the module grade, involves implementing and parallelizing a digital image processor. The main objectives include:

- **Implementing a Thread Pool**: Create a custom thread pool without using existing Java thread pool classes. This involves modifying the ImageProcessorMT class to implement the Runnable interface and managing threads in the thread pool.

- **Parallelizing Image Processing**: Modify the ImageProcessorMT class to perform image filtering and greyscale operations in parallel. This requires distributing image data among multiple threads and reassembling the processed data.