package com.casaverde.backend.fachadaServices.services;

import java.util.List;

import com.casaverde.backend.fachadaServices.DTO.UserDTO;

public interface IUserService {
    public List<UserDTO> findAll();

    public UserDTO findById(Long id);

    public UserDTO save(UserDTO user);

    public UserDTO update(Long id, UserDTO user);

    public boolean delete(Long id);
}
