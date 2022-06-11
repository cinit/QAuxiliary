//
// Created by kinit on 2021-06-24.
//

#ifndef RPCPROTOCOL_SHARED_MEMORY_H
#define RPCPROTOCOL_SHARED_MEMORY_H

#include <cstddef>

bool has_memfd_support();

/*
 * ashmem_create_region - creates a new ashmem region and returns the file
 * descriptor, or <0 on error
 *
 * `name' is an optional label to give the region (visible in /proc/pid/maps)
 * `size' is the size of the region, in page-aligned bytes
 */
int ashmem_create_region(const char *name, size_t size);

/**
 * @param fd origin file
 * @return memfd fd if success, -errno on failure
 */
int copy_file_to_memfd(int fd, const char *name);

#endif //RPCPROTOCOL_SHARED_MEMORY_H
