import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fetchProfile, updateProfile, selectRole } from '../services/profileApi';
import type { UpdateProfilePayload, RoleSelectionPayload } from '../types';
import { useAuth } from '../../auth/hooks/useAuth';

export const useProfile = () => {
  const queryClient = useQueryClient();
  const { isAuthenticated, refreshUser } = useAuth();

  const profileQuery = useQuery({
    queryKey: ['profile'],
    queryFn: fetchProfile,
    enabled: isAuthenticated,
    staleTime: 1000 * 60 * 5, // 5 minutes cache
    retry: 1,
  });

  const updateProfileMutation = useMutation({
    mutationFn: (payload: UpdateProfilePayload) => updateProfile(payload),
    onSuccess: (updatedProfile) => {
      queryClient.setQueryData(['profile'], updatedProfile);
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      refreshUser();
    },
  });

  const selectRoleMutation = useMutation({
    mutationFn: (payload: RoleSelectionPayload) => selectRole(payload),
    onSuccess: (updatedProfile) => {
      queryClient.setQueryData(['profile'], updatedProfile);
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      refreshUser();
    },
  });

  return {
    profile: profileQuery.data,
    isLoading: profileQuery.isLoading,
    isError: profileQuery.isError,
    error: profileQuery.error,
    refetchProfile: profileQuery.refetch,
    updateProfile: updateProfileMutation.mutateAsync,
    isUpdating: updateProfileMutation.isPending,
    updateError: updateProfileMutation.error,
    selectRole: selectRoleMutation.mutateAsync,
    isSelectingRole: selectRoleMutation.isPending,
    selectRoleError: selectRoleMutation.error,
  };
};
